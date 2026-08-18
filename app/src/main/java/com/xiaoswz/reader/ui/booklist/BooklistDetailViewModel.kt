package com.xiaoswz.reader.ui.booklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BooklistDetail
import com.xiaoswz.reader.data.booklist.BooklistRepository
import com.xiaoswz.reader.data.community.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BooklistDetailUiState(
    val detail: BooklistDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountId: String? = null,
)

class BooklistDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BooklistDetailUiState())
    val uiState: StateFlow<BooklistDetailUiState> = _uiState.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            BooklistRepository.getDetail(id).onSuccess { d ->
                _uiState.value = _uiState.value.copy(detail = d, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun setAccountId(id: String?) {
        if (_uiState.value.accountId == id) return
        _uiState.value = _uiState.value.copy(accountId = id)
    }

    /** 编辑书单（标题/简介/封面）。owner/admin。 */
    fun editBooklist(
        id: String,
        title: String,
        description: String?,
        coverUrl: String?,
        onResult: (Result<Boolean>) -> Unit,
    ) {
        viewModelScope.launch {
            BooklistRepository.editBooklist(id, title, description, coverUrl)
                .onSuccess { ok -> onResult(Result.success(ok)); load(id) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    /** 删除书单（软删）。owner/admin。 */
    fun deleteBooklist(id: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            BooklistRepository.deleteBooklist(id)
                .onSuccess { ok -> onResult(Result.success(ok)) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    /** 更新书单项（编辑推荐语 / 调整排序）。owner/admin。 */
    fun updateItem(
        id: String,
        itemId: String,
        note: String? = null,
        position: Int? = null,
        onResult: (Result<Boolean>) -> Unit = {},
    ) {
        viewModelScope.launch {
            BooklistRepository.updateBooklistItem(id, itemId, note, position)
                .onSuccess { ok -> onResult(Result.success(ok)); load(id) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    /** 收藏 / 取消收藏 */
    fun toggleCollect(id: String, onResult: (Result<Pair<Boolean, Int>>) -> Unit) {
        viewModelScope.launch {
            BooklistRepository.collect(id).onSuccess { resp ->
                _uiState.value = _uiState.value.copy(
                    detail = _uiState.value.detail?.copy(
                        collected = resp.collected,
                        collectCount = resp.collectCount,
                    ),
                )
                onResult(Result.success(resp.collected to resp.collectCount))
            }.onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    fun addItem(
        id: String,
        bookSourceId: String,
        bookId: String,
        title: String?,
        author: String?,
        coverUrl: String?,
        note: String?,
        onResult: (Result<Boolean>) -> Unit,
    ) {
        viewModelScope.launch {
            BooklistRepository.addItem(id, bookSourceId, bookId, title, author, coverUrl, note)
                .onSuccess { ok ->
                    onResult(Result.success(ok))
                    load(id)
                }.onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    fun deleteItem(id: String, itemId: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            BooklistRepository.deleteItem(id, itemId).onSuccess { ok ->
                onResult(Result.success(ok))
                load(id)
            }.onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    /** 分享书单到书友圈 */
    fun shareToCommunity(id: String, title: String, description: String?, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            CommunityRepository.shareBooklist(id, title, description)
                .onSuccess { onResult(Result.success(it.ok)) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    fun report(id: String, reason: String?, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            com.xiaoswz.reader.data.social.SocialRepository.reportBooklist(id, reason)
                .onSuccess { onResult(Result.success(it)) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }
}
