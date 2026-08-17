package com.xiaoswz.reader.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.AdminBookDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 书籍元数据编辑（管理台，App 内嵌）：搜索书籍 → 编辑书名/作者/封面/隐藏。
 * 不触碰小说正文（阅读器仍从主站书源取正文）。
 */
data class BookAdminUiState(
    val query: String = "",
    val books: List<AdminBookDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val editing: AdminBookDto? = null,
    val saving: Boolean = false,
)

class BookAdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BookAdminUiState())
    val uiState: StateFlow<BookAdminUiState> = _uiState.asStateFlow()

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun search(q: String = _uiState.value.query) {
        _uiState.update { it.copy(page = 1, isLoading = true, error = null) }
        viewModelScope.launch {
            val res = BackendRepository.adminListBooks(q.takeIf { s -> s.isNotBlank() }, 1)
            if (res.isSuccess) {
                val body = res.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = body?.books ?: emptyList(),
                        total = body?.total ?: 0,
                        page = 1,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "搜索失败") }
            }
        }
    }

    fun nextPage() {
        val cur = _uiState.value
        if (cur.isLoading || cur.books.isEmpty()) return
        val next = cur.page + 1
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val res = BackendRepository.adminListBooks(cur.query.takeIf { s -> s.isNotBlank() }, next)
            if (res.isSuccess) {
                val body = res.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = body?.books ?: emptyList(),
                        total = body?.total ?: 0,
                        page = next,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "翻页失败") }
            }
        }
    }

    fun startEdit(book: AdminBookDto) {
        _uiState.update { it.copy(editing = book) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editing = null) }
    }

    fun save(title: String, author: String, coverUrl: String, hidden: Boolean) {
        val book = _uiState.value.editing ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val res = BackendRepository.adminPatchBook(
                src = book.bookSourceId,
                id = book.bookId,
                title = title.takeIf { t -> t.isNotBlank() },
                author = author.takeIf { a -> a.isNotBlank() },
                coverUrl = coverUrl.takeIf { c -> c.isNotBlank() },
                hidden = hidden,
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(saving = false, editing = null, toast = "已保存") }
                search(_uiState.value.query)
            } else {
                _uiState.update { it.copy(saving = false) }
                val msg = when (val t = res.exceptionOrNull()) {
                    is HttpException -> when (t.code()) {
                        403 -> "无权限（请用管理员账号登录）"
                        401 -> "登录已失效，请重新登录"
                        else -> "保存失败，请稍后重试"
                    }
                    else -> "保存失败，后端未连接"
                }
                _uiState.update { it.copy(toast = msg) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
