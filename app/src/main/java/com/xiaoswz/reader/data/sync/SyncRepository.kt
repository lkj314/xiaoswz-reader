package com.xiaoswz.reader.data.sync

import android.content.Context
import android.util.Log
import com.xiaoswz.reader.data.api.BackendApi
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.BookIdBody
import com.xiaoswz.reader.data.api.BookshelfUpsertBody
import com.xiaoswz.reader.data.api.DeviceIdBody
import com.xiaoswz.reader.data.api.ProgressUpsertBody
import com.xiaoswz.reader.data.bookshelf.BookEntity
import com.xiaoswz.reader.data.bookshelf.BookUpdateStore
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.first

/**
 * 云端同步仓库（离线优先）。
 *
 * 设计原则：
 * 1. 绝不因后端不可达/异常而崩溃 —— 所有网络调用包在 try 中，失败仅记日志并返回失败结果。
 * 2. 不修改 Room 表结构（铁律）：同步状态放 DataStore，书架/进度仍走 Room。
 * 3. 全量同步 + LWW：拉取时云端较新才覆盖本地；推送时本地较新才覆盖云端。
 * 4. 删除传播：记录"上次已同步 slug 集合"，本地移除的书会从云端删掉。
 *
 * 书源只有 xiaoswz 一个，故 bookSourceId 固定；后端 bookId 即本地 slug。
 */
class SyncRepository(context: Context) {

    private val appSettings = AppSettingsRepository(context.applicationContext)
    private val shelf = BookshelfRepository(context.applicationContext)

    companion object {
        private const val BOOK_SOURCE = "xiaoswz"
        private const val SYNC_INTERVAL_MS = 30 * 60 * 1000L // 启动自动同步最小间隔 30 分钟
        private const val TAG = "SyncRepository"
    }

    data class SyncResult(
        val bookshelfCount: Int = 0,
        val progressCount: Int = 0,
        val message: String = "",
    )

    /** 启动时的节流同步：自动同步开启且距上次超过间隔才执行 */
    suspend fun syncIfNeeded() {
        try {
            if (!appSettings.getAutoSync()) return
            // 云同步仅对已登录账号开放；游客直接跳过，不触发任何网络请求
            if (appSettings.getAuthToken().isNullOrBlank()) return
            val last = appSettings.getLastSyncAt()
            if (System.currentTimeMillis() - last < SYNC_INTERVAL_MS) return
            syncNow()
        } catch (t: Throwable) {
            Log.w(TAG, "syncIfNeeded skipped: ${t.message}")
        }
    }

    /**
     * 执行一次完整同步（离线安全）。
     * 流程：确保设备ID → 设后端地址/设备头（Bearer 已注入）→ 拉取云端合并 → 推送本地全量+删除传播 → 记录时间。
     * 游客（无 token）直接失败，提示先登录。
     */
    suspend fun syncNow(): Result<SyncResult> {
        // 云同步需登录账号（user / admin）；游客不可使用
        if (appSettings.getAuthToken().isNullOrBlank()) {
            return Result.failure(SecurityException("请先登录后再使用云同步"))
        }
        return try {
            val deviceId = appSettings.getDeviceId()
            BackendClient.setDeviceId(deviceId)
            BackendClient.setBaseUrl(appSettings.getBackendBaseUrl())
            val api = BackendClient.api

            // 确保后端存在该设备用户（无状态，失败也不影响后续离线使用）
            runCatching { api.anonLogin(DeviceIdBody(deviceId)) }

            // 1) 拉取云端全量并合并到本地
            val remote = api.pullSync(null)
            mergeRemote(remote)

            // 2) 推送本地全量书架/进度 + 删除传播
            // 修复（M10）：统计推送失败条数；部分失败时不刷新 lastSyncAt 并返回失败，
            // 否则节流窗口内不会重试，UI 还会谎报「同步成功」。
            val failed = pushLocal(api)
            if (failed > 0) {
                Log.w(TAG, "sync incomplete: $failed item(s) failed to push")
                return Result.failure(
                    IllegalStateException("同步未完成：$failed 条数据推送失败，请稍后重试"),
                )
            }

            appSettings.setLastSyncAt(System.currentTimeMillis())
            Result.success(
                SyncResult(
                    bookshelfCount = remote.bookshelf.size,
                    progressCount = remote.progress.size,
                    message = "同步成功",
                ),
            )
        } catch (t: Throwable) {
            Log.w(TAG, "sync failed (offline-safe): ${t.message}")
            Result.failure(t)
        }
    }

    /** 把云端数据按 LWW 合并进本地 Room（云端较新才覆盖） */
    private suspend fun mergeRemote(remote: com.xiaoswz.reader.data.api.SyncResponse) {
        val localBooks = shelf.observeAll().first()
        val localBySlug = localBooks.associateBy { it.slug }

        for (rb in remote.bookshelf) {
            val slug = rb.bookId
            val local = localBySlug[slug]
            // 软删墓碑：某端删了该书，需在本端同步清除本地副本（删除传播）。
            if (rb.deleted) {
                if (local != null) shelf.remove(slug)
                continue
            }
            // 封面只接受 http(s)，data: URI 不写回本地
            val cover = if (rb.coverUrl?.startsWith("data:") == true) null else rb.coverUrl
            if (local == null) {
                shelf.add(
                    BookEntity(
                        slug = slug,
                        title = rb.title,
                        author = rb.author,
                        coverUrl = cover,
                        firstChapterId = null,
                        lastChapterId = null,
                        lastChapterTitle = null,
                        addedAt = rb.updatedAt,
                        lastReadAt = rb.updatedAt,
                    ),
                )
            } else if (rb.updatedAt >= local.lastReadAt) {
                // 云端较新：覆盖元数据；封面策略（0.16.2 修正）：
                // 云端为 null/blank 或 data: 时**保留本地封面**（保护本地缩略图缓存，
                // 否则云端 null 会把本地已缓存的 data: 缩略图覆盖清空，导致每次启动重拉）；
                // 云端为 http(s) 时采用（跨设备一致、Coil 可缓存）。
                shelf.update(
                    local.copy(
                        title = rb.title,
                        author = rb.author,
                        coverUrl = if (rb.coverUrl.isNullOrBlank() || rb.coverUrl.startsWith("data:", ignoreCase = true)) local.coverUrl else rb.coverUrl,
                        lastReadAt = rb.updatedAt,
                    ),
                )
            }
        }

        // 进度合并（重新读取，包含上面新增的本地书）
        val localAfter = shelf.observeAll().first().associateBy { it.slug }
        for (rp in remote.progress) {
            val local = localAfter[rp.bookId] ?: continue
            if (rp.updatedAt >= local.lastReadAt && !rp.chapterId.isNullOrBlank()) {
                // 修复（M9）：第三个参数原先恒传 null，会把本地已有的章节标题覆盖清空。
                // 云端进度不带标题，故用本地现有标题兜底。
                shelf.updateProgress(rp.bookId, rp.chapterId, local.lastChapterTitle, rp.updatedAt)
            }
        }
    }

    /**
     * 推送本地全量到云端，并删除云端已本地移除的书。
     * 返回推送失败条数（0 = 全部成功），供调用方决定是否刷新同步时间（M10）。
     */
    private suspend fun pushLocal(api: BackendApi): Int {
        var failed = 0
        val localBooks = shelf.observeAll().first()
        val localSlugs = localBooks.map { it.slug }.toSet()
        val pushedSlugs = mutableSetOf<String>()

        for (b in localBooks) {
            val cover = if (b.coverUrl?.startsWith("data:") == true) null else b.coverUrl
            runCatching {
                api.upsertBookshelf(
                    BookshelfUpsertBody(
                        bookSourceId = BOOK_SOURCE,
                        bookId = b.slug,
                        title = b.title,
                        author = b.author,
                        coverUrl = cover,
                        status = "reading",
                        updatedAt = b.lastReadAt,
                    ),
                )
            }.onSuccess { pushedSlugs.add(b.slug) }
                .onFailure { failed++ } // 修复（M10）：不再静默吞掉失败，累计条数
            // 仅当本地有阅读进度时才推送进度（无 lastChapterId 不推送）
            if (!b.lastChapterId.isNullOrBlank()) {
                runCatching {
                    api.upsertProgress(
                        ProgressUpsertBody(
                            bookSourceId = BOOK_SOURCE,
                            bookId = b.slug,
                            chapterId = b.lastChapterId,
                            // 修复（M9）：原先恒传 0，会把云端进度覆盖回「第 0 章 / 0%」
                            chapterIndex = chapterIndexFor(b),
                            progressPercent = b.progressPercent,
                            updatedAt = b.lastReadAt,
                        ),
                    )
                }.onFailure { failed++ }
            }
        }

        // 删除传播：上次同步推送过、但本次本地已无 → 云端删除
        val synced = appSettings.getSyncedSlugs()
        val toDelete = synced - localSlugs

        // 修复（H4）护栏 1：本地书架为空时绝不传播删除。AppDatabase 用破坏性迁移，
        // schema 变更或清数据后 Room 会全空，此时 synced - localSlugs = 云端全部书籍，
        // 逐条删除会让云端书架被永久清空且不可回滚。保留 synced 记录不动。
        if (localSlugs.isEmpty()) {
            if (toDelete.isNotEmpty()) {
                Log.w(TAG, "skip delete propagation: local bookshelf empty, keep ${toDelete.size} remote item(s)")
            }
            return failed
        }
        // 修复（H4）护栏 2：待删除占比超过一半，多半是本地数据异常（而非用户真的删了这么多书），
        // 同样跳过删除并记日志，避免异常批量删除。
        if (synced.isNotEmpty() && toDelete.size * 2 > synced.size) {
            Log.w(TAG, "skip delete propagation: too many (${toDelete.size}/${synced.size})")
            return failed
        }

        for (slug in toDelete) {
            runCatching { api.deleteBookshelf(BookIdBody(BOOK_SOURCE, slug)) }
                .onFailure { failed++ }
        }
        // 记录：本次推送成功的 + 本地仍保留且此前已同步的（推送失败的保留旧记录，下次重试）
        appSettings.setSyncedSlugs(pushedSlugs + (synced - toDelete))
        return failed
    }

    /**
     * 本地只持久化了进度百分比（BookEntity.progressPercent），没有存章节序号，
     * 故按「百分比 × 已知总章节数」反推当前章节下标；总章节数未知（<=0）时记 0。
     */
    private fun chapterIndexFor(b: BookEntity): Int {
        val total = runCatching { BookUpdateStore.getKnown(b.slug) }.getOrNull() ?: 0
        if (total <= 0 || b.progressPercent <= 0) return 0
        return (b.progressPercent * total / 100 - 1).coerceAtLeast(0)
    }
}
