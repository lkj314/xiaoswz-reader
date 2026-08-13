package com.xiaoswz.reader.data

import com.xiaoswz.reader.data.api.ApiClient
import com.xiaoswz.reader.data.api.XiaoswzApi
import com.xiaoswz.reader.data.model.BookDetailDto
import com.xiaoswz.reader.data.model.BookListResponse
import com.xiaoswz.reader.data.model.ChapterContentDto

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

    suspend fun getChapterContent(chapterId: String): Result<ChapterContentDto> = runCatching {
        api.getChapterContent(chapterId = chapterId)
    }

    fun clearCache() {
        detailCache.clear()
    }
}
