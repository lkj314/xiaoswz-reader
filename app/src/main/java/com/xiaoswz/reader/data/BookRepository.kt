package com.xiaoswz.reader.data

import com.xiaoswz.reader.data.api.ApiClient
import com.xiaoswz.reader.data.api.XiaoswzApi
import com.xiaoswz.reader.data.cache.ChapterCacheManager
import com.xiaoswz.reader.data.model.BookDetailDto
import com.xiaoswz.reader.data.model.BookListResponse
import com.xiaoswz.reader.data.model.ChapterContentDto

/**
 * 章节加载结果：data 为章节正文，fromOfflineCache=true 表示内容来自磁盘文件（断网可读）
 */
data class ChapterResult(
    val data: ChapterContentDto,
    val fromOfflineCache: Boolean,
)

/**
 * 书籍数据仓库
 * 所有数据来自冲浪中文网公开 API（只读）
 */
class BookRepository(
    private val api: XiaoswzApi = ApiClient.api,
) {

    /**
     * 详情内存缓存：阅读器翻上一章/下一章需要目录数据，
     * 避免每次翻页都重新拉取整本书目录
     */
    private val detailCache = mutableMapOf<String, BookDetailDto>()

    /**
     * 章节正文缓存：支持预读（提前拉取下一章，翻章零等待）
     */
    private val contentCache = mutableMapOf<String, ChapterContentDto>()

    suspend fun getBooks(
        page: Int,
        sort: String,
        search: String? = null,
    ): Result<BookListResponse> = runCatching {
        api.getBooks(
            page = page,
            sort = sort,
            search = search?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    suspend fun getBookDetail(slug: String): Result<BookDetailDto> {
        detailCache[slug]?.let { return Result.success(it) }
        return runCatching {
            api.getBookDetail(bookId = slug).also { detail ->
                detailCache[slug] = detail
            }
        }
    }

    /**
     * 章节正文三级缓存：内存 → 文件离线缓存 → 网络。
     * 已缓存章节杀进程重进后断网仍可读取。
     */
    suspend fun getChapterContent(chapterId: String): Result<ChapterResult> {
        contentCache[chapterId]?.let { return Result.success(ChapterResult(it, false)) }
        ChapterCacheManager.get(chapterId)?.let { return Result.success(ChapterResult(it, true)) }
        return runCatching {
            val content = api.getChapterContent(chapterId = chapterId)
            contentCache[chapterId] = content
            ChapterCacheManager.put(content)
            content
        }.map { ChapterResult(it, false) }
    }

    /** 预取章节正文（不阻塞，失败静默）；命中会落文件缓存，支持离线 */
    suspend fun prefetchChapter(chapterId: String) {
        if (contentCache.containsKey(chapterId)) return
        runCatching { getChapterContent(chapterId) }
    }

    fun clearCache() {
        detailCache.clear()
        contentCache.clear()
    }
}
