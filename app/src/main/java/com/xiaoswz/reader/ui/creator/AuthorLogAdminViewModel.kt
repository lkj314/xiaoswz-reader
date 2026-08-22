package com.xiaoswz.reader.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.AdminBookDto
import com.xiaoswz.reader.data.api.AuthorLogDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 作者日志管理（管理台，App 内嵌，仅 admin）：先搜索并选择一本书，
 * 再对这书的日志做 新建 / 编辑 / 删除。三类：碎碎念 / 公告 / 章节改动。
 */
data class AuthorLogAdminUiState(
    val query: String = "",
    val bookResults: List<AdminBookDto> = emptyList(),
    val selectedBookId: String? = null,
    val selectedBookTitle: String? = null,
    val logs: List<AuthorLogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val showEditor: Boolean = false,
    val editing: AuthorLogDto? = null, // null = 新增
    val saving: Boolean = false,
)

class AuthorLogAdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthorLogAdminUiState())
    val uiState: StateFlow<AuthorLogAdminUiState> = _uiState.asStateFlow()

    fun searchBooks(q: String) {
        _uiState.update { it.copy(query = q) }
        if (q.isBlank()) {
            _uiState.update { it.copy(bookResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            BackendRepository.adminListBooks(q, 1)
                .onSuccess { resp -> _uiState.update { it.copy(bookResults = resp.books) } }
                .onFailure { _uiState.update { it.copy(bookResults = emptyList()) } }
        }
    }

    fun selectBook(book: AdminBookDto) {
        _uiState.update {
            it.copy(
                selectedBookId = book.bookId,
                selectedBookTitle = book.title,
                bookResults = emptyList(),
                query = "",
                logs = emptyList(),
            )
        }
        loadLogs()
    }

    fun loadLogs() {
        val bookId = _uiState.value.selectedBookId ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            BackendRepository.getAuthorLogs(bookId, page = 1)
                .onSuccess { resp -> _uiState.update { it.copy(isLoading = false, logs = resp.items) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, error = "日志加载失败") } }
        }
    }

    fun startAdd() {
        if (_uiState.value.selectedBookId == null) {
            _uiState.update { it.copy(toast = "请先搜索并选择一本书") }
            return
        }
        _uiState.update { it.copy(showEditor = true, editing = null) }
    }

    fun startEdit(log: AuthorLogDto) {
        _uiState.update { it.copy(showEditor = true, editing = log) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(showEditor = false, editing = null) }
    }

    fun save(type: String, title: String, body: String, chapterRef: String?, pinned: Boolean) {
        val t = title.trim()
        val b = body.trim()
        if (t.isEmpty() || b.isEmpty()) {
            _uiState.update { it.copy(toast = "标题与正文均不能为空") }
            return
        }
        val bookId = _uiState.value.selectedBookId ?: return
        val editing = _uiState.value.editing
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val res = if (editing == null) {
                BackendRepository.adminCreateAuthorLog(
                    bookId,
                    type,
                    t,
                    b,
                    chapterRef?.trim()?.ifEmpty { null },
                    pinned,
                )
            } else {
                BackendRepository.adminPatchAuthorLog(
                    editing.id,
                    t,
                    b,
                    type,
                    chapterRef?.trim()?.ifEmpty { null },
                    pinned,
                )
            }
            if (res.isSuccess) {
                _uiState.update { it.copy(saving = false, showEditor = false, editing = null, toast = "已保存") }
                loadLogs()
            } else {
                _uiState.update { it.copy(saving = false) }
                _uiState.update {
                    it.copy(
                        toast = when (val e = res.exceptionOrNull()) {
                            is HttpException -> when (e.code()) {
                                403 -> "无权限（请用管理员账号登录）"
                                401 -> "登录已失效，请重新登录"
                                else -> "保存失败，请稍后重试"
                            }
                            else -> "保存失败，后端未连接"
                        },
                    )
                }
            }
        }
    }

    fun delete(log: AuthorLogDto) {
        viewModelScope.launch {
            val res = BackendRepository.adminDeleteAuthorLog(log.id)
            if (res.isSuccess) {
                _uiState.update { it.copy(toast = "已删除") }
                loadLogs()
            } else {
                _uiState.update {
                    it.copy(
                        toast = when (val e = res.exceptionOrNull()) {
                            is HttpException -> when (e.code()) {
                                403 -> "无权限（请用管理员账号登录）"
                                401 -> "登录已失效，请重新登录"
                                else -> "删除失败，请稍后重试"
                            }
                            else -> "删除失败，后端未连接"
                        },
                    )
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
