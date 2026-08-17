package com.xiaoswz.reader.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.AdminAnnouncementDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 公告管理（管理台，App 内嵌）：列表 + 新建 / 编辑 / 删除。
 * level: info / warning / important。
 */
data class AnnouncementAdminUiState(
    val announcements: List<AdminAnnouncementDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val showEditor: Boolean = false,
    val editing: AdminAnnouncementDto? = null, // null = 新增
    val saving: Boolean = false,
)

class AnnouncementAdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementAdminUiState())
    val uiState: StateFlow<AnnouncementAdminUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val res = BackendRepository.adminListAnnouncements()
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        announcements = res.getOrNull()?.announcements ?: emptyList(),
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "公告加载失败") }
            }
        }
    }

    fun startAdd() {
        _uiState.update { it.copy(showEditor = true, editing = null) }
    }

    fun startEdit(a: AdminAnnouncementDto) {
        _uiState.update { it.copy(showEditor = true, editing = a) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(showEditor = false, editing = null) }
    }

    fun save(title: String, body: String, level: String) {
        val t = title.trim()
        val b = body.trim()
        if (t.isEmpty() || b.isEmpty()) {
            _uiState.update { it.copy(toast = "标题与正文均不能为空") }
            return
        }
        val editing = _uiState.value.editing
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val res = if (editing == null) {
                BackendRepository.adminCreateAnnouncement(t, b, level)
            } else {
                BackendRepository.adminPatchAnnouncement(editing.id, t, b, level)
            }
            if (res.isSuccess) {
                _uiState.update { it.copy(saving = false, showEditor = false, editing = null, toast = "已保存") }
                load()
            } else {
                _uiState.update { it.copy(saving = false) }
                val msg = when (val e = res.exceptionOrNull()) {
                    is HttpException -> when (e.code()) {
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

    fun delete(a: AdminAnnouncementDto) {
        viewModelScope.launch {
            val res = BackendRepository.adminDeleteAnnouncement(a.id)
            if (res.isSuccess) {
                _uiState.update { it.copy(toast = "已删除") }
                load()
            } else {
                val msg = when (val e = res.exceptionOrNull()) {
                    is HttpException -> when (e.code()) {
                        403 -> "无权限（请用管理员账号登录）"
                        401 -> "登录已失效，请重新登录"
                        else -> "删除失败，请稍后重试"
                    }
                    else -> "删除失败，后端未连接"
                }
                _uiState.update { it.copy(toast = msg) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
