package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BookCoinBalance
import com.xiaoswz.reader.data.api.CoinBalanceDto
import com.xiaoswz.reader.data.api.CoinLedgerDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 书币账本（0.17.0 → 0.18 多书币）：聚合余额 + 按书持仓 + 透明流水（借鉴点4：被看见即激励）。
 * 每本书有独立的硬通货书币池，互不相同；用户跨书持有多种书币。
 */
data class CoinUiState(
    val balance: Int = 0, // 聚合可用余额
    val earnedTotal: Int = 0, // 累计获得
    val locked: Int = 0, // 聚合锁仓
    val coins: List<BookCoinBalance> = emptyList(), // 0.18 按书持仓明细
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
                val b: CoinBalanceDto = bal.getOrDefault(CoinBalanceDefault)
                val l = led.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balance = b.balance,
                        earnedTotal = b.earnedTotal,
                        locked = b.locked,
                        coins = b.coins,
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
