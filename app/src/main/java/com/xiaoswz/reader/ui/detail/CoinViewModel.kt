package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.CoinLedgerDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 书币账本（0.17.0）：余额 + 透明流水（借鉴点4：被看见即激励）。
 * 书币不可交易不可提现，只能通过真实行为（阅读时长、有效章评、角色贡献、投资分红等）获得。
 */
data class CoinUiState(
    val balance: Int = 0,
    val earnedTotal: Int = 0,
    val ledger: List<CoinLedgerDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class CoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CoinUiState())
    val uiState: StateFlow<CoinUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val bal = BackendRepository.getCoinBalance()
            val led = BackendRepository.getCoinLedger(1)
            if (bal.isSuccess && led.isSuccess) {
                val b = bal.getOrDefault(CoinBalanceDefault)
                val l = led.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balance = b.balance,
                        earnedTotal = b.earnedTotal,
                        ledger = l.items,
                        total = l.total,
                        page = 1,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "账本加载失败，后端未连接或网络异常") }
            }
        }
    }

    fun loadMore() {
        val cur = _uiState.value
        if (cur.isLoading) return
        if (cur.ledger.size >= cur.total) return
        val next = cur.page + 1
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            BackendRepository.getCoinLedger(next)
                .onSuccess { l ->
                    _uiState.update {
                        it.copy(isLoading = false, ledger = it.ledger + l.items, page = next, total = l.total)
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }
}

private val CoinBalanceDefault = com.xiaoswz.reader.data.api.CoinBalanceDto()
