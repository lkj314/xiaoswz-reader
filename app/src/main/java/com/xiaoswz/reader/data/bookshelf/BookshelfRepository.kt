package com.xiaoswz.reader.data.bookshelf

import android.content.Context
import com.xiaoswz.reader.data.model.shrinkCover
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
     * 一次性恢复：把过大的 data: 封面清空，避免 CursorWindow 溢出导致书架整体崩溃。
     * 仅清空封面列，书籍元信息（slug/标题/进度）全部保留 —— 不丢书。
     * UPDATE 语句不返回游标窗口，故即使某行巨大也不会触发 SQLiteBlobTooBigException。
     */
    fun blankOversizedCovers() {
        try {
            db.openHelper.writableDatabase
                .execSQL("UPDATE bookshelf SET cover_url = '' WHERE cover_url LIKE 'data:%'")
        } catch (_: Throwable) {
            // 表不存在等极端情况忽略
        }
    }

    /**
     * 重新拉取封面（按 slug）并压缩成缩略图写回，恢复书架封面显示。
     * 先 blank 一遍确保大封面已清空，使下方 observeAll() 不会崩溃；
     * 之后只对封面为空的书籍生效（已正常的不会重复拉取）。需联网；离线时跳过，下次再试。
     */
    suspend fun repairCovers(fetchCover: suspend (slug: String) -> String?) {
        blankOversizedCovers()
        try {
            val books = dao.observeAll().first()
            for (b in books) {
                if (b.coverUrl.isNullOrBlank()) {
                    val remote = try { fetchCover(b.slug) } catch (_: Throwable) { null }
                    val small = shrinkCover(remote)
                    if (!small.isNullOrBlank()) {
                        dao.update(b.copy(coverUrl = small))
                    }
                }
            }
        } catch (_: Throwable) {
            // 忽略单次修复失败
        }
    }
}
