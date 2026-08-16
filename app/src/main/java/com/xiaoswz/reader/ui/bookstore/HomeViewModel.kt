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
 * 首页 ViewModel：只拉取「热度最高」的若干书（sort=popular）供轮播展示，
 * 保持首页轻量整洁。其余发现类操作（排行榜 / 最新上架 / 分区浏览）在书库页完成。
 */
data class HomeUiState(
    val books: List<BookDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** 重新加载热度榜首屏（下拉刷新时调用） */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBooks(page = 1, sort = "popular", search = null)
                .onSuccess { resp ->
                    _uiState.update {
                        it.copy(books = resp.books.orEmpty(), isLoading = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "网络错误，请稍后重试")
                    }
                }
        }
    }
}
