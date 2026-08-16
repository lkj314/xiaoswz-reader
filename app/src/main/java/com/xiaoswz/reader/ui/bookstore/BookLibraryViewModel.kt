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

/**
 * 书库 ViewModel（0.8.2 由「书城」拆出）：承载排行榜之外的发现类操作——
 * 分区浏览（category）/ 状态筛选 / 排序 / 搜索 / 分页，全部走冲浪阅读自有 BookCatalog。
 */
data class BookLibraryUiState(
    val books: List<BookDto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val sort: String = SORT_LATEST,
    val category: String = "all",
    val page: Int = 1,
    val totalPages: Int = 1,
) {
    val canLoadMore: Boolean get() = page < totalPages

    companion object {
        const val SORT_LATEST = "latest"
        const val SORT_POPULAR = "popular"
    }
}

class BookLibraryViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookLibraryUiState())
    val uiState: StateFlow<BookLibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** 重新加载第一页（刷新 / 搜索 / 切换排序 / 切换分区时调用） */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBooks(
                page = 1,
                sort = state.sort,
                search = state.query,
                category = state.category,
            ).onSuccess { resp ->
                _uiState.update {
                    it.copy(
                        books = resp.books.orEmpty(),
                        page = resp.page ?: 1,
                        totalPages = resp.totalPages ?: 1,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
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
            repository.getBooks(
                page = nextPage,
                sort = state.sort,
                search = state.query,
                category = state.category,
            ).onSuccess { resp ->
                _uiState.update {
                    it.copy(
                        books = it.books + resp.books.orEmpty(),
                        page = resp.page ?: nextPage,
                        totalPages = resp.totalPages ?: it.totalPages,
                        isLoadingMore = false,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onSortChange(sort: String) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        refresh()
    }

    fun onCategoryChange(category: String) {
        if (_uiState.value.category == category) return
        _uiState.update { it.copy(category = category) }
        refresh()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
