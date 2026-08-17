package com.xiaoswz.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.api.BookSegmentCommentItem
import com.xiaoswz.reader.data.api.SegmentCommentItem
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 段评独立全屏页（v0.15.3）：跨章节聚合「根段评」，起点/番茄风格讨论流。
 *
 * 后端物理隔离无 Chapter 表（章节来自书源），故章节标题由 App 用本地已加载目录
 * （[com.xiaoswz.reader.data.model.BookDetailDto.chapters]）做 chapterId→name 映射，
 * 聚合端点只返回根段评（parentId=null 且 paragraphIndex 非空），含 replyCount 楼中楼数。
 *
 * 打开某条根段评 → 拉取该章全部段评并过滤到该段落 → 复用 [SegmentThreadSheet] 渲染
 * 主楼 + 楼中楼，发表/回复/点赞/举报与主阅读器内逻辑一致。
 */
data class SegmentCommentListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val comments: List<BookSegmentCommentItem> = emptyList(),
    val chapterNames: Map<String, String> = emptyMap(),
    val hasMore: Boolean = false,
    val nextCursor: Long? = null,
    val toast: String? = null,
)

/** 当前打开的单段线程（主楼 + 楼中楼），由 SegmentThreadSheet 渲染。 */
data class ActiveSegmentThread(
    val chapterId: String,
    val paragraphIndex: Int,
    val quote: String,
    val comments: List<SegmentCommentItem> = emptyList(),
    val isLoading: Boolean = false,
)

class SegmentCommentListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SegmentCommentListUiState())
    val uiState: StateFlow<SegmentCommentListUiState> = _uiState.asStateFlow()

    private val _threadState = MutableStateFlow<ActiveSegmentThread?>(null)
    val threadState: StateFlow<ActiveSegmentThread?> = _threadState.asStateFlow()

    private var bookSlug: String = ""
    private val bookRepo = BookRepository()

    fun load(bookSlug: String) {
        this.bookSlug = bookSlug
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // 并行：全书段评聚合 + 书籍目录（用于章节标题映射）
            val agg = BackendRepository.getBookSegmentComments(bookSlug)
            val detail = bookRepo.getBookDetail(bookSlug)
            val chapterNames = detail.getOrNull()?.chapters?.filterNotNull()
                ?.mapNotNull { c -> c.id?.let { id -> id to (c.name ?: "") } }
                ?.toMap() ?: emptyMap()
            val resp = agg.getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    comments = resp?.comments ?: emptyList(),
                    hasMore = resp?.hasMore ?: false,
                    nextCursor = resp?.nextCursor,
                    chapterNames = chapterNames,
                    error = if (agg.isFailure) "段评加载失败，请稍后重试" else null,
                )
            }
        }
    }

    fun loadMore() {
        val cur = _uiState.value
        if (cur.isLoading || !cur.hasMore || cur.nextCursor == null) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val resp = BackendRepository.getBookSegmentComments(bookSlug, cur.nextCursor).getOrNull()
            if (resp == null) {
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        comments = it.comments + resp.comments,
                        hasMore = resp.hasMore,
                        nextCursor = resp.nextCursor,
                    )
                }
            }
        }
    }

    /** 打开某条根段评对应的单段线程（拉取该章段评并过滤到该段落）。 */
    fun openThread(chapterId: String, paragraphIndex: Int, quote: String) {
        if (bookSlug.isBlank()) return
        _threadState.value = ActiveSegmentThread(chapterId, paragraphIndex, quote, isLoading = true)
        viewModelScope.launch {
            val all = BackendRepository.getSegmentComments(bookSlug, chapterId).getOrNull()?.comments
                ?: emptyList()
            val thread = all.filter { it.paragraphIndex == paragraphIndex }
            _threadState.value = _threadState.value?.copy(comments = thread, isLoading = false)
        }
    }

    fun closeThread() {
        _threadState.value = null
        // 关闭线程后刷新聚合列表，更新楼中楼数
        if (bookSlug.isNotBlank()) load(bookSlug)
    }

    fun likeComment(id: String) {
        viewModelScope.launch { BackendRepository.likeComment(id) }
    }

    fun reportComment(id: String) {
        viewModelScope.launch { BackendRepository.reportComment(id) }
    }

    fun postSegmentComment(
        chapterId: String,
        paragraphIndex: Int,
        quote: String,
        content: String,
        parentId: String? = null,
    ) {
        val text = content.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val appSettings = AppSettingsRepository(AppContext.app)
            if (appSettings.getAccountRole() == "guest") {
                _uiState.update { it.copy(toast = "请先登录后再评论") }
                return@launch
            }
            if (appSettings.isMuted()) {
                _uiState.update { it.copy(toast = "您已被禁言，暂时无法评论") }
                return@launch
            }
            val res = BackendRepository.postSegmentComment(
                bookSlug, chapterId, text, paragraphIndex, null, null, quote, parentId,
            )
            if (res.isFailure) {
                handleLoginRequired(res, "评论")
            } else {
                // 刷新当前线程，使新评论立即可见
                val cur = _threadState.value
                if (cur != null && cur.chapterId == chapterId && cur.paragraphIndex == paragraphIndex) {
                    openThread(chapterId, paragraphIndex, quote)
                }
                _uiState.update { it.copy(toast = "评论已发布") }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    private fun handleLoginRequired(res: Result<*>, action: String) {
        val msg = when (val t = res.exceptionOrNull()) {
            is HttpException -> when (t.code()) {
                403 -> "请先登录后再${action}"
                401 -> "登录已失效，请重新登录"
                else -> "${action}失败，请稍后重试"
            }
            else -> "${action}失败，后端未连接"
        }
        _uiState.update { it.copy(toast = msg) }
    }
}
