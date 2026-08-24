package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.CreateFundResponse
import com.xiaoswz.reader.data.api.FundAssetInput
import com.xiaoswz.reader.data.api.FundDiscoveryResponse
import com.xiaoswz.reader.data.api.FundSummaryDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 理财产品发现（0.19）：市场列表 + 「我的持仓」合并展示。
 * 董事可发起新产品（每个书圈资产包 = 多书币按权重捆绑的理财产品）。
 */
data class FundListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val funds: List<FundSummaryDto> = emptyList(),
    val canCreate: Boolean = false, // 是否为某书圈董事（可发起产品）
    val showCreate: Boolean = false,
    val createBookId: String = "",
    val createName: String = "",
    val createDesc: String = "",
    val createAssets: String = "", // 格式：B000001:60,B000002:40（换行/分号亦可）
    val creating: Boolean = false,
)

class FundListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FundListUiState())
    val uiState: StateFlow<FundListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val disc = BackendRepository.getFundDiscovery()
            val hub = BackendRepository.getCircleHub()
            disc.onSuccess { d ->
                val canCreate = hub.getOrNull()?.books?.any { it.isDirector } ?: false
                _uiState.update { it.copy(isLoading = false, funds = d.funds, canCreate = canCreate) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "理财产品市场加载失败，后端未连接或网络异常") }
            }
        }
    }

    fun toggleCreate() = _uiState.update { it.copy(showCreate = !it.showCreate) }
    fun setCreateBookId(v: String) = _uiState.update { it.copy(createBookId = v) }
    fun setCreateName(v: String) = _uiState.update { it.copy(createName = v) }
    fun setCreateDesc(v: String) = _uiState.update { it.copy(createDesc = v) }
    fun setCreateAssets(v: String) = _uiState.update { it.copy(createAssets = v) }

    fun createFund() {
        val s = _uiState.value
        val bookId = s.createBookId.trim()
        val name = s.createName.trim()
        if (bookId.isEmpty() || name.isEmpty()) {
            _uiState.update { it.copy(toast = "请填写书 ID 与产品名称") }
            return
        }
        val assets = parseAssets(s.createAssets)
        if (assets.isEmpty()) {
            _uiState.update { it.copy(toast = "请至少填写一项资产，格式：B000001:60,B000002:40") }
            return
        }
        _uiState.update { it.copy(creating = true) }
        viewModelScope.launch {
            BackendRepository.createFund(bookId, name, s.createDesc.trim().ifBlank { null }, assets)
                .onSuccess { r ->
                    if (r.ok) {
                        _uiState.update {
                            it.copy(
                                creating = false, showCreate = false,
                                createBookId = "", createName = "", createDesc = "", createAssets = "",
                                toast = "理财产品《$name》已发起，去详情页认购买入吧",
                            )
                        }
                        load()
                    } else {
                        _uiState.update { it.copy(creating = false, toast = "发起失败") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(creating = false, toast = "发起失败：${e.message ?: "网络异常"}") }
                }
        }
    }

    private fun parseAssets(text: String): List<FundAssetInput> {
        return text.split(',', '\n', '；', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { seg ->
                val idx = seg.lastIndexOf(':')
                if (idx <= 0 || idx >= seg.length - 1) return@mapNotNull null
                val bookId = seg.substring(0, idx).trim()
                val w = seg.substring(idx + 1).trim().toDoubleOrNull() ?: return@mapNotNull null
                FundAssetInput(bookId, w)
            }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }
}
