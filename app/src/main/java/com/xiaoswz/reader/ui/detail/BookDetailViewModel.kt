package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.model.BookDetailDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: BookDetailDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class BookDetailViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(slug: String) {
        // 同一本书不重复加载
        if (_uiState.value.detail?.id == slug) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBookDetail(slug)
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败，请稍后重试")
                    }
                }
        }
    }

    fun retry(slug: String) {
        _uiState.update { it.copy(detail = null) }
        load(slug)
    }
}
