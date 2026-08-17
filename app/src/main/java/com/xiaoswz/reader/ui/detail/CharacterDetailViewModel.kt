package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.api.CharacterDetailResponse
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
 * 角色详情页状态：比心 + 标签墙 + 角色讨论（0.14.0）。
 * 角色列表本身由 [BookDetailViewModel] 加载；本 VM 只负责单个角色的互动数据。
 */
data class CharacterUiState(
    val character: CharacterDetailResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val comments: List<CommentItem> = emptyList(),
    val commentTotal: Int = 0,
    val toast: String? = null,
    val postingTag: Boolean = false,
    val newTagName: String = "",
)

class CharacterDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    fun load(characterId: String) {
        if (_uiState.value.character?.id == characterId && !_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val detail = BackendRepository.getCharacterDetail(characterId).getOrNull()
            val comments = BackendRepository.getCharacterComments(characterId).getOrNull()
            if (detail == null) {
                _uiState.update { it.copy(isLoading = false, error = "角色加载失败，请稍后重试") }
            } else {
                _uiState.update {
                    it.copy(
                        character = detail,
                        isLoading = false,
                        comments = comments?.comments ?: emptyList(),
                        commentTotal = comments?.total ?: 0,
                    )
                }
            }
        }
    }

    /** 比心切换：乐观更新本地计数，登录态缺失时引导登录 */
    fun toggleHeart() {
        val ch = _uiState.value.character ?: return
        val id = ch.id
        viewModelScope.launch {
            val res = BackendRepository.toggleHeart(id)
            if (res.isSuccess) {
                val r = res.getOrNull()
                _uiState.update { s ->
                    s.copy(
                        character = s.character?.copy(
                            myHeart = r?.hearted ?: false,
                            heartCount = r?.heartCount ?: s.character.heartCount,
                        ),
                    )
                }
            } else {
                handleLoginRequired(res, "比心")
            }
        }
    }

    /** 新建角色标签（登录 + 长度 1-12 + 禁言拦截） */
    fun createTag(name: String) {
        val ch = _uiState.value.character ?: return
        val text = name.trim()
        if (text.isEmpty() || text.length > 12) {
            _uiState.update { it.copy(toast = "标签需 1-12 个字") }
            return
        }
        viewModelScope.launch {
            val appSettings = AppSettingsRepository(AppContext.app)
            when (appSettings.getAccountRole()) {
                "guest" -> {
                    _uiState.update { it.copy(toast = "请先登录后再添加标签") }
                    return@launch
                }
            }
            if (appSettings.isMuted()) {
                _uiState.update { it.copy(toast = "您已被禁言，暂时无法操作") }
                return@launch
            }
            _uiState.update { it.copy(postingTag = true) }
            val res = BackendRepository.createTag(ch.id, text)
            if (res.isSuccess) {
                refreshDetail(ch.id)
                _uiState.update { it.copy(postingTag = false, newTagName = "") }
                _uiState.update { it.copy(toast = "标签已添加") }
            } else {
                _uiState.update { it.copy(postingTag = false) }
                handleLoginRequired(res, "添加标签")
            }
        }
    }

    /** 标签投票切换（登录） */
    fun toggleTagVote(tagId: String) {
        val ch = _uiState.value.character ?: return
        viewModelScope.launch {
            val res = BackendRepository.toggleTagVote(tagId)
            if (res.isSuccess) {
                refreshDetail(ch.id)
            } else {
                handleLoginRequired(res, "投票")
            }
        }
    }

    /** 发角色讨论（登录 + 禁言拦截），沿用先放后审 */
    fun postComment(content: String) {
        val ch = _uiState.value.character ?: return
        val id = ch.id
        val text = content.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val appSettings = AppSettingsRepository(AppContext.app)
            when (appSettings.getAccountRole()) {
                "guest" -> {
                    _uiState.update { it.copy(toast = "请先登录后再评论") }
                    return@launch
                }
            }
            if (appSettings.isMuted()) {
                _uiState.update { it.copy(toast = "您已被禁言，暂时无法评论") }
                return@launch
            }
            val res = BackendRepository.postCharacterComment(id, text)
            if (res.isSuccess) {
                val comments = BackendRepository.getCharacterComments(id).getOrNull()
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

    fun onNewTagNameChange(name: String) {
        _uiState.update { it.copy(newTagName = name) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    /** 重新拉取角色详情（比心/标签变动后刷新标签墙排序与投票态） */
    private fun refreshDetail(characterId: String) {
        viewModelScope.launch {
            val detail = BackendRepository.getCharacterDetail(characterId).getOrNull()
            if (detail != null) _uiState.update { it.copy(character = detail) }
        }
    }

    /** 区分后端明确拒绝（403 登录失效 / 禁言）与网络不可达 */
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
