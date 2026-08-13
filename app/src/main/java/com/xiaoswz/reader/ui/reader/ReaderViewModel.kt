package com.xiaoswz.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val chapterTitle: String = "",
    val bookName: String = "",
    val content: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentIndex: Int = 0,
    val totalChapters: Int = 0,
    val prevChapterId: String? = null,
    val nextChapterId: String? = null,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val isDark: Boolean = false,
) {
    companion object {
        const val DEFAULT_FONT_SIZE = 18
        const val MIN_FONT_SIZE = 14
        const val MAX_FONT_SIZE = 28
    }
}

class ReaderViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun load(bookSlug: String, chapterId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // 正文 + 目录并行加载（目录用于计算上一章/下一章）
            val contentResult = repository.getChapterContent(chapterId)
            val detailResult = repository.getBookDetail(bookSlug)

            contentResult
                .onSuccess { chapter ->
                    val chapters = detailResult.getOrNull()?.chapters.orEmpty()
                    val index = chapters.indexOfFirst { it.id == chapterId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chapterTitle = chapter.title.orEmpty(),
                            bookName = chapter.bookName.orEmpty(),
                            content = chapter.content.orEmpty(),
                            currentIndex = if (index >= 0) index + 1 else 0,
                            totalChapters = chapters.size,
                            prevChapterId = if (index > 0) chapters.getOrNull(index - 1)?.id else null,
                            nextChapterId = if (index >= 0) chapters.getOrNull(index + 1)?.id else null,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "章节加载失败")
                    }
                }
        }
    }

    fun retry(bookSlug: String, chapterId: String) = load(bookSlug, chapterId)

    fun increaseFontSize() {
        _uiState.update {
            it.copy(fontSize = (it.fontSize + 1).coerceAtMost(ReaderUiState.MAX_FONT_SIZE))
        }
    }

    fun decreaseFontSize() {
        _uiState.update {
            it.copy(fontSize = (it.fontSize - 1).coerceAtLeast(ReaderUiState.MIN_FONT_SIZE))
        }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDark = !it.isDark) }
    }
}
