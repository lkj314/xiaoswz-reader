package com.xiaoswz.reader.data

import com.xiaoswz.reader.data.api.ApiClient
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.CatalogListResponse
import com.xiaoswz.reader.data.api.XiaoswzApi
import com.xiaoswz.reader.data.cache.BookMetaCache
import com.xiaoswz.reader.data.cache.CatalogCache
import com.xiaoswz.reader.data.cache.ChapterCacheManager
import com.xiaoswz.reader.data.model.AuthorDto
import com.xiaoswz.reader.data.model.BookDetailDto
import com.xiaoswz.reader.data.model.BookDto
import com.xiaoswz.reader.data.model.BookListResponse
import com.xiaoswz.reader.data.model.CategoryDto
import com.xiaoswz.reader.data.model.ChapterContentDto
import com.xiaoswz.reader.data.model.ChapterDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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
        category: String? = null,
    ): Result<BookListResponse> {
        val q = search?.trim()?.takeIf { it.isNotEmpty() }
        val cat = category?.trim()?.takeIf { it.isNotEmpty() && it != "all" }
        // 书城首屏（无搜索、无分类）优先用文件缓存：开书城秒开，30min 内视为新鲜
        if (page == 1 && q == null && cat == null && CatalogCache.isFresh(sort)) {
            CatalogCache.get(sort)?.let { return Result.success(it) }
        }
        // 主路径：读后端自有书库（快，不实时爬主站）
        val backend = runCatching {
            BackendClient.api.getCatalog(
                page = page,
                limit = 24,
                sort = mapCatalogSort(sort),
                search = q,
                category = cat,
            ).let { mapCatalogToBookList(it) }
        }
        if (backend.isSuccess) {
            val resp = backend.getOrThrow()
            if (page == 1 && q == null && cat == null) CatalogCache.put(sort, resp)
            // 后端书库尚未播种（空）时兜底主站，保证可用
            if (resp.books.isNullOrEmpty()) {
                return fetchMainSiteBooks(page, sort, q)
            }
            return Result.success(resp)
        }
        // 后端不可达 → 兜底主站
        return fetchMainSiteBooks(page, sort, q)
    }

    /** 书库分区（分类）列表：来自冲浪阅读自有 BookCatalog，用于「分区浏览」入口 */
    suspend fun getCatalogCategories(): Result<List<String>> = runCatching {
        BackendClient.api.getCatalogCategories().categories
    }

    /** 后端书库排序字段映射到主站排序（保持兜底一致） */
    private fun mapCatalogSort(sort: String): String = when (sort) {
        "popularity", "hot" -> "popular"
        "newest", "updated" -> "updated"
        "words" -> "words"
        else -> "latest"
    }

    /** 把后端 CatalogListResponse 映射成客户端统一的 BookListResponse（BookDto） */
    private fun mapCatalogToBookList(resp: CatalogListResponse): BookListResponse {
        return BookListResponse(
            books = resp.books.map { c ->
                BookDto(
                    id = c.bookId,
                    uid = c.uid,
                    title = c.title,
                    slug = c.bookId,
                    coverImage = c.coverUrl, // 已是 http(s)，Coil 直接加载
                    status = c.status,
                    wordCount = c.wordCount,
                    chapterCount = c.chapterCount,
                    viewCount = c.viewCount,
                    author = AuthorDto(name = c.author),
                    category = CategoryDto(name = c.category),
                )
            },
            total = resp.total,
            page = resp.page,
            totalPages = resp.totalPages,
        )
    }

    /** 主站兜底：直连公开只读 API（仅在后端书库空或不可达时触发） */
    private suspend fun fetchMainSiteBooks(
        page: Int,
        sort: String,
        q: String?,
    ): Result<BookListResponse> {
        return runCatching {
            api.getBooks(page = page, sort = sort, search = q).also { resp ->
                if (page == 1 && q == null) CatalogCache.put(sort, resp)
            }
        }
    }

    suspend fun getBookDetail(slug: String, forceRefresh: Boolean = false): Result<BookDetailDto> {
        if (!forceRefresh) {
            detailCache[slug]?.let { return Result.success(it) }
            if (BookMetaCache.isFresh(slug)) {
                val cached = BookMetaCache.get(slug)
                if (cached != null) {
                    detailCache[slug] = cached
                    return Result.success(cached)
                }
            }
        }
        return runCatching {
            api.getBookDetail(bookId = slug).also { detail ->
                detailCache[slug] = detail
                BookMetaCache.put(slug, detail)
            }
        }.recoverCatching { e ->
            // 网络失败兜底：若有陈旧文件缓存也先拿来开书（断网可读目录）
            BookMetaCache.get(slug)?.also { detailCache[slug] = it } ?: throw e
        }
    }

    /**
     * 章节正文三级缓存：内存 → 文件离线缓存 → 网络。
     * 已缓存章节杀进程重进后断网仍可读取。
     */
    suspend fun getChapterContent(chapterId: String): Result<ChapterResult> {
        contentCache[chapterId]?.let { return Result.success(ChapterResult(it, false)) }
        // 修复：文件缓存读写（含 JSON 解析）改用 suspend 版本，内部切 Dispatchers.IO，不再阻塞主线程
        ChapterCacheManager.getSuspend(chapterId)?.let { return Result.success(ChapterResult(it, true)) }
        return runCatching {
            val content = api.getChapterContent(chapterId = chapterId)
            contentCache[chapterId] = content
            ChapterCacheManager.putSuspend(content)
            content
        }.map { ChapterResult(it, false) }
    }

    /** 预取章节正文（不阻塞，失败静默）；已缓存且新鲜则跳过，减少主站只读接口 hits */
    suspend fun prefetchChapter(chapterId: String) {
        if (contentCache.containsKey(chapterId)) return
        // 修复：isFresh 是 File.exists()+lastModified()，改用 suspend 版本在 IO 线程执行
        if (ChapterCacheManager.isFreshSuspend(chapterId)) return
        runCatching { getChapterContent(chapterId) }
    }

    /**
     * 整本离线下载：并发拉取全部章节正文落本地文件缓存。
     * 并发上限 3，温柔对待主站只读接口；已缓存且新鲜的章节直接跳过。
     * onProgress(done, total) 已切回主线程回调，调用方可直接更新 UI。
     */
    suspend fun downloadWholeBook(
        chapters: List<ChapterDto>,
        onProgress: (done: Int, total: Int) -> Unit,
    ) {
        if (chapters.isEmpty()) return
        val total = chapters.size
        val done = AtomicInteger(0)
        // 分批并发（每批 3），温柔对待主站只读接口；已缓存且新鲜的章节直接跳过。
        // 修复：async 显式指定 Dispatchers.IO —— 原来继承主线程（viewModelScope），
        // 整本下载期间同步文件 IO + 解析全压在主线程导致 UI 冻结。
        coroutineScope {
            chapters.chunked(3).forEach { batch ->
                batch.mapNotNull { ch -> ch.id }.map { id ->
                    async(Dispatchers.IO) {
                        if (!ChapterCacheManager.isFresh(id)) {
                            runCatching { getChapterContent(id) }
                        }
                        // 修复：回调切回主线程，保持与原实现一致（调用方直接更新 UI）
                        withContext(Dispatchers.Main.immediate) {
                            onProgress(done.incrementAndGet(), total)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    fun clearCache() {
        detailCache.clear()
        contentCache.clear()
        BookMetaCache.clear()
        CatalogCache.clear()
    }
}
