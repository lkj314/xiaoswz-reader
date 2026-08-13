package com.xiaoswz.reader.ui.bookstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.model.BookDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookstoreUiState(
    val books: List<BookDto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val sort: String = SORT_LATEST,
    val page: Int = 1,
    val totalPages: Int = 1,
) {
    val canLoadMore: Boolean get() = page < totalPages

    companion object {
        const val SORT_LATEST = "latest"
        const val SORT_POPULAR = "popular"
    }
}

class BookstoreViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookstoreUiState())
    val uiState: StateFlow<BookstoreUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** 重新加载第一页（刷新 / 搜索 / 切换排序时调用） */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBooks(page = 1, sort = state.sort, search = state.query)
                .onSuccess { resp ->
                    _uiState.update {
                        it.copy(
                            books = resp.books.orEmpty(),
                            page = resp.page ?: 1,
                            totalPages = resp.totalPages ?: 1,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "网络错误，请稍后重试")
                    }
                }
        }
    }

    /** 加载下一页 */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val nextPage = state.page + 1
            repository.getBooks(page = nextPage, sort = state.sort, search = state.query)
                .onSuccess { resp ->
                    _uiState.update {
                        it.copy(
                            books = it.books + resp.books.orEmpty(),
                            page = resp.page ?: nextPage,
                            totalPages = resp.totalPages ?: it.totalPages,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun onSortChange(sort: String) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        refresh()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
