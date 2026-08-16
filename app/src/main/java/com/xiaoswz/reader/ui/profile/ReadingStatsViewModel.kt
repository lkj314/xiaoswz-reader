package com.xiaoswz.reader.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.ReadingStats
import com.xiaoswz.reader.data.social.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingStatsUiState(
    val stats: ReadingStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ReadingStatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingStatsUiState())
    val uiState: StateFlow<ReadingStatsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            SocialRepository.getReadingStats()
                .onSuccess { s -> _uiState.value = _uiState.value.copy(stats = s, isLoading = false) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败") }
        }
    }
}
