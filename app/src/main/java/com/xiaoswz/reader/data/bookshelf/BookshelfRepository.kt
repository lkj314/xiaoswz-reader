package com.xiaoswz.reader.data.bookshelf

import android.content.Context
import com.xiaoswz.reader.data.model.shrinkCover
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.roundToInt

/**
 * 本地书架仓库：封装 Room，对外提供收藏与阅读进度的读写。
 * 调用方直接传入 applicationContext 创建即可（内部为单例数据库）。
 */
class BookshelfRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.bookDao()

    /** 书架列表（响应式，收藏/移除/进度更新自动刷新 UI） */
    fun observeAll(): Flow<List<BookEntity>> = dao.observeAll()

    suspend fun getBySlug(slug: String): BookEntity? = dao.getBySlug(slug)

    suspend fun isCollected(slug: String): Boolean = dao.exists(slug)

    suspend fun add(book: BookEntity) = dao.insert(book)

    suspend fun remove(slug: String) = dao.deleteBySlug(slug)

    /** 整体更新一本书（同步合并时覆盖元数据用） */
    suspend fun update(book: BookEntity) = dao.update(book)

    /** 记录阅读进度：仅更新已收藏书籍，未收藏则不产生数据 */
    suspend fun updateProgress(slug: String, chapterId: String, chapterTitle: String? = null) =
        dao.updateProgress(slug, chapterId, chapterTitle, System.currentTimeMillis())

    /** 带显式时间戳的阅读进度更新（云同步合并时保留远端时间，用于 LWW） */
    suspend fun updateProgress(
        slug: String,
        chapterId: String,
        chapterTitle: String? = null,
        ts: Long,
    ) = dao.updateProgress(slug, chapterId, chapterTitle, ts)

    /**
     * 阅读器记录进度时一并写入进度百分比（0.16.0）。
     * 百分比由 currentIndex/totalChapters 推算；totalChapters<=0（未知）时记为 0。
     */
    suspend fun updateReadingProgress(
        slug: String,
        chapterId: String,
        chapterTitle: String? = null,
        currentIndex: Int = 0,
        totalChapters: Int = 0,
        ts: Long = System.currentTimeMillis(),
    ) {
        // 修复（L8）：原来是 `(currentIndex + 1) * 100 / totalChapters` 的整型除法，
        // 章数 > 100 时前若干章一律被截断为 0（300 章读到第 1 章 → 1*100/300 = 0），
        // 书架长期显示 0%。改用浮点计算后四舍五入。
        val pct = if (totalChapters > 0) {
            ((currentIndex + 1) * 100.0 / totalChapters).roundToInt().coerceIn(0, 100)
        } else 0
        dao.updateProgressWithPercent(slug, chapterId, chapterTitle, pct, ts)
    }

    /**
     * 设置阅读状态（0.16.0）。finished 时进度置 100；切换回在读/想读时保留已有进度百分比。
     */
    suspend fun setStatus(slug: String, status: String) {
        val cur = dao.getBySlug(slug)
        val pct = if (status == "finished") 100 else (cur?.progressPercent ?: 0)
        dao.updateStatus(slug, status, pct)
    }

    /**
     * 一次性恢复：只把**过大**的 data: 封面清空，避免 CursorWindow 溢出导致书架整体崩溃。
     * 仅清空封面列，书籍元信息（slug/标题/进度）全部保留 —— 不丢书。
     *
     * ⚠️ 历史坑（0.16.2 修正）：旧逻辑是 `WHERE cover_url LIKE 'data:%'`（清空*所有* data 封面），
     * 但 repairCovers 写回的缩略图本身也是 `data:image/jpeg;base64,...`，于是每次启动/打开书架
     * 都被这里清空、又被 repairCovers 重新联网拉取，缓存永远失效 —— 这就是「每次登录封面加载
     * 十几秒」的根因。现改为只清空超过 256KB 的 data 封面（原始未压缩的几 MB 大图才会超限），
     * 已压缩的缩略图（通常 < 56KB）永久保留，缓存命中、本地秒开。
     * UPDATE 语句不返回游标窗口，故即使某行巨大也不会触发 SQLiteBlobTooBigException。
     */
    fun blankOversizedCovers() {
        try {
            db.openHelper.writableDatabase
                .execSQL("UPDATE bookshelf SET cover_url = '' WHERE cover_url LIKE 'data:%' AND LENGTH(cover_url) > 262144")
        } catch (_: Throwable) {
            // 表不存在等极端情况忽略
        }
    }

    /**
     * 重新拉取封面（按 slug）并压缩成缩略图写回，恢复书架封面显示。
     * 先 blank 一遍确保过大的 data 封面已清空，使下方 observeAll() 不会崩溃；
     * 之后只对封面为空的书籍生效（已正常的不会重复拉取）。需联网；离线时跳过，下次再试。
     *
     * 改进（0.16.2）：并发限制为 4，避免一次性对几十本书各发 getBookDetail
     * （每本响应含几 MB 的 data URI 封面）打爆书源 API、造成首页卡死；
     * 且只在确实有缺失封面时动手，缓存命中时（绝大多数情况）几乎零开销。
     */
    suspend fun repairCovers(fetchCover: suspend (slug: String) -> String?) {
        blankOversizedCovers()
        try {
            val books = dao.observeAll().first()
            val missing = books.filter { it.coverUrl.isNullOrBlank() }
            if (missing.isEmpty()) return
            val sem = Semaphore(4)
            coroutineScope {
                missing.map { b ->
                    async {
                        sem.withPermit {
                            val remote = try { fetchCover(b.slug) } catch (_: Throwable) { null }
                            val small = shrinkCover(remote)
                            if (!small.isNullOrBlank()) dao.update(b.copy(coverUrl = small))
                        }
                    }
                }.awaitAll()
            }
        } catch (_: Throwable) {
            // 忽略单次修复失败
        }
    }
}
