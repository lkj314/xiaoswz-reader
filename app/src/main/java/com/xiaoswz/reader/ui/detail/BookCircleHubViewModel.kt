package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.CircleHubResponse
import com.xiaoswz.reader.data.api.HubBookDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 书圈专区（0.18）：导航【书圈】Tab 的聚合页。
 * 统计用户已投资 / 已加入的书籍，换算持股占比与资产净值，呈现「金融仪表盘」式总览。
 */
data class CircleHubUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val hub: CircleHubResponse? = null,
)

class BookCircleHubViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CircleHubUiState())
    val uiState: StateFlow<CircleHubUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            BackendRepository.getCircleHub()
                .onSuccess { hub -> _uiState.update { it.copy(isLoading = false, hub = hub) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, error = "书圈专区加载失败，后端未连接或网络异常") } }
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }
}

/** 角色 → 中文标签（金融模拟器：董事会语境） */
val dirRoleLabel = mapOf(
    "owner" to "董事长",
    "council" to "议事员",
    "elder" to "长老",
    "member" to "书迷",
)
