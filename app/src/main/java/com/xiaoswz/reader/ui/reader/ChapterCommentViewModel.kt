package com.xiaoswz.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.api.CommentItem
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 章评状态（v0.15）：锚定到章节。列表/发表/点赞/举报，沿用角色讨论的先放后审与降级范式。
 */
data class ChapterCommentUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val comments: List<CommentItem> = emptyList(),
    val commentTotal: Int = 0,
    val toast: String? = null,
)

class ChapterCommentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterCommentUiState())
    val uiState: StateFlow<ChapterCommentUiState> = _uiState.asStateFlow()
    private var bookSlug: String = ""
    private var chapterId: String = ""

    fun load(bookSlug: String, chapterId: String) {
        this.bookSlug = bookSlug
        this.chapterId = chapterId
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val comments = BackendRepository.getChapterComments(bookSlug, chapterId).getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    comments = comments?.comments ?: emptyList(),
                    commentTotal = comments?.total ?: 0,
                )
            }
        }
    }

    fun postComment(content: String) {
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
            val res = BackendRepository.postChapterComment(bookSlug, chapterId, text)
            if (res.isSuccess) {
                val comments = BackendRepository.getChapterComments(bookSlug, chapterId).getOrNull()
                _uiState.update {
                    it.copy(
                        comments = comments?.comments ?: emptyList(),
                        commentTotal = comments?.total ?: 0,
                        toast = "评论已发布",
                    )
                }
            } else {
                handleLoginRequired(res, "评论")
            }
        }
    }

    fun likeComment(id: String) {
        viewModelScope.launch {
            BackendRepository.likeComment(id)
            _uiState.update { s ->
                s.copy(comments = s.comments.map { if (it.id == id) it.copy(likeCount = it.likeCount + 1) else it })
            }
        }
    }

    fun reportComment(id: String) {
        viewModelScope.launch {
            BackendRepository.reportComment(id)
            _uiState.update { it.copy(toast = "已举报，感谢反馈") }
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
