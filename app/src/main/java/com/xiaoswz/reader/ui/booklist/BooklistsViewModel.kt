package com.xiaoswz.reader.ui.booklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.data.booklist.BooklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BooklistsUiState(
    val scope: String = "all",
    val lists: List<BooklistSummary> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val accountId: String? = null,
)

class BooklistsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BooklistsUiState())
    val uiState: StateFlow<BooklistsUiState> = _uiState.asStateFlow()

    fun switchScope(scope: String) {
        if (_uiState.value.scope == scope) return
        _uiState.value = _uiState.value.copy(scope = scope)
        load(refresh = true)
    }

    fun load(refresh: Boolean = false) {
        val scope = _uiState.value.scope
        val page = if (refresh) 1 else _uiState.value.page + 1
        if (!refresh && _uiState.value.page >= _uiState.value.totalPages) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = refresh,
                isLoadingMore = !refresh,
                error = null,
            )
            BooklistRepository.getBooklists(scope, page).onSuccess { resp ->
                val merged = if (refresh) resp.booklists else _uiState.value.lists + resp.booklists
                val tp = if (resp.pageSize <= 0) 1 else kotlin.math.ceil(resp.total.toDouble() / resp.pageSize).toInt()
                _uiState.value = _uiState.value.copy(
                    lists = merged,
                    page = resp.page,
                    totalPages = tp,
                    isLoading = false,
                    isLoadingMore = false,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "加载失败",
                )
            }
        }
    }

    fun create(title: String, description: String?, coverUrl: String?, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            BooklistRepository.createBooklist(title, description, coverUrl).onSuccess { id ->
                onResult(Result.success(id))
                load(refresh = true)
            }.onFailure { e -> onResult(Result.failure(e)) }
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
                .onSuccess { ok -> onResult(Result.success(ok)); load(refresh = true) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    /** 删除书单（软删）。owner/admin。 */
    fun deleteBooklist(id: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            BooklistRepository.deleteBooklist(id)
                .onSuccess { ok -> onResult(Result.success(ok)); load(refresh = true) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }
}
