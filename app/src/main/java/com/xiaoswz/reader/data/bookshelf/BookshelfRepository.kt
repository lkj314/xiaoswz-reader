package com.xiaoswz.reader.data.bookshelf

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 本地书架仓库：封装 Room，对外提供收藏与阅读进度的读写。
 * 调用方直接传入 applicationContext 创建即可（内部为单例数据库）。
 */
class BookshelfRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).bookDao()

    /** 书架列表（响应式，收藏/移除/进度更新自动刷新 UI） */
    fun observeAll(): Flow<List<BookEntity>> = dao.observeAll()

    suspend fun getBySlug(slug: String): BookEntity? = dao.getBySlug(slug)

    suspend fun isCollected(slug: String): Boolean = dao.exists(slug)

    suspend fun add(book: BookEntity) = dao.insert(book)

    suspend fun remove(slug: String) = dao.deleteBySlug(slug)

    /** 记录阅读进度：仅更新已收藏书籍，未收藏则不产生数据 */
    suspend fun updateProgress(slug: String, chapterId: String, chapterTitle: String? = null) =
        dao.updateProgress(slug, chapterId, chapterTitle, System.currentTimeMillis())
}
