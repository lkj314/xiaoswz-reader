package com.xiaoswz.reader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.model.ChapterDto
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val bookSlug: String = "",
    val currentChapterId: String = "",
    val chapterTitle: String = "",
    val bookName: String = "",
    val rawContent: String = "",
    val toc: List<ChapterDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val menuVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val settings: ReaderSettings = ReaderSettings(),
) {
    val currentIndex: Int get() = toc.indexOfFirst { it.id == currentChapterId }.let { if (it < 0) 0 else it }
    val totalChapters: Int get() = toc.size
    val prevChapterId: String? get() = toc.getOrNull(toc.indexOfFirst { it.id == currentChapterId } - 1)?.id
    val nextChapterId: String? get() = toc.getOrNull(toc.indexOfFirst { it.id == currentChapterId } + 1)?.id
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    // repository 在类体内初始化，确保构造函数只剩单一 Application 参数，
    // 否则 AndroidViewModelFactory.getConstructor(Application.class) 反射失败 →
    // "Cannot create an instance of ReaderViewModel" 闪退（点开章节必崩）。
    private val repository = BookRepository()
    private val settingsRepo = ReaderSettingsRepository(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        // 订阅设置流，实时应用到阅读器
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    /** 入口：加载书籍目录 + 指定章节 */
    fun load(bookSlug: String, chapterId: String) {
        if (_uiState.value.bookSlug == bookSlug && _uiState.value.toc.isNotEmpty()) {
            // 同一本书已就绪，直接切章
            if (_uiState.value.currentChapterId != chapterId) loadChapter(chapterId)
            return
        }
        _uiState.update { it.copy(bookSlug = bookSlug, isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBookDetail(bookSlug)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            toc = detail.chapters.orEmpty(),
                            bookName = detail.name.orEmpty(),
                        )
                    }
                    loadChapter(chapterId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "目录加载失败")
                    }
                }
        }
    }

    private fun loadChapter(chapterId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getChapterContent(chapterId)
                .onSuccess { chapter ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentChapterId = chapterId,
                            chapterTitle = chapter.title.orEmpty(),
                            rawContent = chapter.content.orEmpty(),
                            bookName = it.bookName.ifBlank { chapter.bookName.orEmpty() },
                            menuVisible = false,
                            settingsVisible = false,
                        )
                    }
                    // 预读下一章，翻章零等待
                    _uiState.value.nextChapterId?.let { nextId ->
                        launch { repository.prefetchChapter(nextId) }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "章节加载失败")
                    }
                }
        }
    }

    fun goToChapter(chapterId: String) = loadChapter(chapterId)

    /** 翻到下一章；没有下一章返回 false */
    fun nextChapter(): Boolean =
        _uiState.value.nextChapterId?.let { loadChapter(it); true } ?: false

    /** 翻到上一章；没有上一章返回 false */
    fun prevChapter(): Boolean =
        _uiState.value.prevChapterId?.let { loadChapter(it); true } ?: false

    fun retry() {
        val state = _uiState.value
        if (state.toc.isEmpty()) {
            load(state.bookSlug, state.currentChapterId)
        } else {
            loadChapter(state.currentChapterId)
        }
    }

    fun toggleMenu() {
        _uiState.update { it.copy(menuVisible = !it.menuVisible, settingsVisible = false) }
    }

    fun hideMenu() {
        _uiState.update { it.copy(menuVisible = false, settingsVisible = false) }
    }

    fun showSettings() {
        _uiState.update { it.copy(settingsVisible = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(settingsVisible = false) }
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }
}
