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
            pushLocal(api)

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
                shelf.updateProgress(rp.bookId, rp.chapterId, null, rp.updatedAt)
            }
        }
    }

    /** 推送本地全量到云端，并删除云端已本地移除的书 */
    private suspend fun pushLocal(api: BackendApi) {
        val localBooks = shelf.observeAll().first()
        val localSlugs = localBooks.map { it.slug }.toSet()

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
            }
            // 仅当本地有阅读进度时才推送进度（无 lastChapterId 不推送）
            if (!b.lastChapterId.isNullOrBlank()) {
                runCatching {
                    api.upsertProgress(
                        ProgressUpsertBody(
                            bookSourceId = BOOK_SOURCE,
                            bookId = b.slug,
                            chapterId = b.lastChapterId,
                            chapterIndex = 0,
                            progressPercent = 0,
                            updatedAt = b.lastReadAt,
                        ),
                    )
                }
            }
        }

        // 删除传播：上次同步推送过、但本次本地已无 → 云端删除
        val synced = appSettings.getSyncedSlugs()
        for (slug in synced - localSlugs) {
            runCatching { api.deleteBookshelf(BookIdBody(BOOK_SOURCE, slug)) }
        }
        appSettings.setSyncedSlugs(localSlugs)
    }
}
