package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.CreateFundResponse
import com.xiaoswz.reader.data.api.FundAssetInput
import com.xiaoswz.reader.data.api.FundDiscoveryResponse
import com.xiaoswz.reader.data.api.FundSummaryDto
import com.xiaoswz.reader.data.api.HubBookDto
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.backend.backendFriendlyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 理财产品发现（0.19）：市场列表 + 「我的持仓」合并展示。
 * 董事可发起新产品：先选「自己是董事的书圈」，再选一个「理财包模版」（模版自动填好资产构成），
 * 命名后即可发起。模版有解锁条件，降低上手门槛、增加游戏感。
 */
data class FundListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val funds: List<FundSummaryDto> = emptyList(),
    val canCreate: Boolean = false, // 是否为某书圈董事（可发起产品）
    val showCreate: Boolean = false,
    // ── 发起表单（书圈 + 模版驱动，杜绝手填错误导致的 403）──
    val directorBooks: List<HubBookDto> = emptyList(), // 你是董事的书圈（可发起产品）
    val allBooks: List<HubBookDto> = emptyList(),      // 你的全部持仓书（模版展开用）
    val subscribedFundCount: Int = 0,                   // 已认购产品数（模版解锁条件）
    val templates: List<FundTemplateUi> = emptyList(),  // 带解锁状态的模版列表
    val selectedBookId: String = "",
    val selectedTemplateId: String = "",
    val createName: String = "",
    val createDesc: String = "",
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
                val h = hub.getOrNull()
                val directorBooks = h?.books?.filter { it.isDirector } ?: emptyList()
                val allBooks = h?.books ?: emptyList()
                val subscribed = h?.funds?.size ?: 0
                val ctx = computeTemplateContext(directorBooks, allBooks, subscribed, h?.anchorBalance ?: 0)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        funds = d.funds,
                        canCreate = directorBooks.isNotEmpty(),
                        directorBooks = directorBooks,
                        allBooks = allBooks,
                        subscribedFundCount = subscribed,
                        templates = buildTemplateUiList(ctx),
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "理财产品市场加载失败，后端未连接或网络异常") }
            }
        }
    }

    fun toggleCreate() = _uiState.update { it.copy(showCreate = !it.showCreate) }
    fun selectBook(bookId: String) = _uiState.update { it.copy(selectedBookId = bookId) }
    fun selectTemplate(templateId: String) = _uiState.update { it.copy(selectedTemplateId = templateId) }
    fun setCreateName(v: String) = _uiState.update { it.copy(createName = v) }
    fun setCreateDesc(v: String) = _uiState.update { it.copy(createDesc = v) }

    fun createFund() {
        val s = _uiState.value
        val bookId = s.selectedBookId
        val name = s.createName.trim()
        val tmpl = FUND_TEMPLATES.firstOrNull { it.id == s.selectedTemplateId }
        // 校验：必须是董事书 + 已选中且已解锁的模版
        val isDirectorBook = s.directorBooks.any { it.bookId == bookId }
        val tmplUi = s.templates.firstOrNull { it.id == s.selectedTemplateId }
        if (!isDirectorBook) {
            _uiState.update { it.copy(toast = "请选择你是董事的书圈") }
            return
        }
        if (name.isEmpty()) {
            _uiState.update { it.copy(toast = "请填写产品名称") }
            return
        }
        if (tmpl == null || tmplUi == null || !tmplUi.unlocked) {
            _uiState.update { it.copy(toast = "请选择一个可用的理财包模版") }
            return
        }
        val assets = tmpl.buildAssets(bookId, s.allBooks)
        if (assets.isEmpty()) {
            _uiState.update { it.copy(toast = "模版资产为空，换个书圈或模版试试") }
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
                                selectedBookId = "", selectedTemplateId = "", createName = "", createDesc = "",
                                toast = "理财产品《$name》已发起，去详情页认购买入吧",
                            )
                        }
                        load()
                    } else {
                        _uiState.update { it.copy(creating = false, toast = "发起失败") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(creating = false, toast = "发起失败：${backendFriendlyError(e)}") }
                }
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }
}
