package com.xiaoswz.reader.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.api.AdminCharacterDto
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 角色录入（管理台，App 内嵌）：为指定书籍增 / 改 / 删角色。
 * 后端 admin 接口以 Bearer 鉴权，角色从 DB 取；当前账号须为 admin（入口已在设置页按 role 拦截）。
 */
data class CharacterAdminUiState(
    val characters: List<AdminCharacterDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val showEditor: Boolean = false,
    val editing: AdminCharacterDto? = null, // null = 新增
    val saving: Boolean = false,
)

class CharacterAdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterAdminUiState())
    val uiState: StateFlow<CharacterAdminUiState> = _uiState.asStateFlow()

    private var currentBookId: String = ""

    fun load(bookId: String) {
        currentBookId = bookId
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val res = BackendRepository.adminListCharacters(bookId)
            if (res.isSuccess) {
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        characters = res.getOrNull()?.characters ?: emptyList(),
                    )
                }
            } else {
                _uiState.update { s -> s.copy(isLoading = false, error = "角色加载失败") }
            }
        }
    }

    fun startAdd() {
        _uiState.update { it.copy(showEditor = true, editing = null) }
    }

    fun startEdit(ch: AdminCharacterDto) {
        _uiState.update { it.copy(showEditor = true, editing = ch) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(showEditor = false, editing = null) }
    }

    fun save(
        name: String,
        roleType: String,
        avatarUrl: String?,
        description: String?,
    ) {
        val bookId = currentBookId
        val text = name.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(toast = "角色名不能为空") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val res = BackendRepository.adminUpsertCharacter(
                bookId = bookId,
                name = text,
                roleType = roleType,
                avatarUrl = avatarUrl?.takeIf { u -> u.isNotBlank() },
                description = description?.takeIf { d -> d.isNotBlank() },
                order = 0,
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(saving = false, showEditor = false, editing = null, toast = "已保存") }
                load(bookId)
            } else {
                _uiState.update { it.copy(saving = false) }
                handleError(res, "保存角色")
            }
        }
    }

    fun delete(ch: AdminCharacterDto) {
        viewModelScope.launch {
            val res = BackendRepository.adminDeleteCharacter(ch.id)
            if (res.isSuccess) {
                _uiState.update { it.copy(toast = "已删除") }
                load(currentBookId)
            } else {
                handleError(res, "删除角色")
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    private fun handleError(res: Result<*>, action: String) {
        val msg = when (val t = res.exceptionOrNull()) {
            is HttpException -> when (t.code()) {
                403 -> "无权限（请用管理员账号登录）"
                401 -> "登录已失效，请重新登录"
                else -> "${action}失败，请稍后重试"
            }
            else -> "${action}失败，后端未连接"
        }
        _uiState.update { it.copy(toast = msg) }
    }
}
