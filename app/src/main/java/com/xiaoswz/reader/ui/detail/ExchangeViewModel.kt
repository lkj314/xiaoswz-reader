package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.ExchangeOrderDto
import com.xiaoswz.reader.data.api.PricePointDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 书币交易所（0.18）：限价订单簿撮合。maker 挂出 fromBook 求 toBook（挂即锁仓），taker 吃单互换。
 * 系统零费率（exchangeFeePct=0）。所有币价由市场成交决定，董事会锚定价仅为信号。
 */
data class ExchangeUiState(
    val fromBookId: String = "",
    val toBookId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val saving: Boolean = false,
    val orders: List<ExchangeOrderDto> = emptyList(),
    val prices: List<PricePointDto> = emptyList(),
    val balances: Map<String, Int> = emptyMap(), // bookId -> 余额（锁仓外的可用余额）
    val fromAmount: String = "",
    val toAmount: String = "",
    val myUserId: String? = null,
)

class ExchangeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExchangeUiState())
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    fun init(fromBookId: String, toBookId: String) {
        _uiState.update { it.copy(fromBookId = fromBookId, toBookId = toBookId) }
        loadAll()
    }

    fun loadAll() {
        val (from, to) = _uiState.value.fromBookId to _uiState.value.toBookId
        if (from.isBlank() || to.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val bal = BackendRepository.getCoinBalance()
            bal.onSuccess { b ->
                val map = b.coins.associate { it.bookId to it.balance }
                _uiState.update { it.copy(balances = map, myUserId = b.userId) }
            }
            val ob = BackendRepository.getOrderBook(from, to)
            ob.onSuccess { resp -> _uiState.update { it.copy(orders = resp.orders) } }
            val ph = BackendRepository.getPriceHistory(from, 60)
            ph.onSuccess { resp -> _uiState.update { it.copy(prices = resp.points) } }
            _uiState.update { it.copy(isLoading = false, error = if (bal.isFailure && ob.isFailure) "交易所数据加载失败，后端未连接" else null) }
        }
    }

    fun swapPair() {
        val s = _uiState.value
        _uiState.update { it.copy(fromBookId = s.toBookId, toBookId = s.fromBookId, fromAmount = "", toAmount = "", orders = emptyList(), prices = emptyList()) }
        loadAll()
    }

    fun setFromBookId(v: String) {
        _uiState.update { it.copy(fromBookId = v.trim(), orders = emptyList(), prices = emptyList()) }
        loadAll()
    }

    fun setToBookId(v: String) {
        _uiState.update { it.copy(toBookId = v.trim(), orders = emptyList(), prices = emptyList()) }
        loadAll()
    }

    fun setFromAmount(v: String) = _uiState.update { it.copy(fromAmount = v) }
    fun setToAmount(v: String) = _uiState.update { it.copy(toAmount = v) }

    fun placeOrder() {
        val s = _uiState.value
        val from = s.fromAmount.toIntOrNull()
        val to = s.toAmount.toIntOrNull()
        if (from == null || from <= 0 || to == null || to <= 0) {
            _uiState.update { it.copy(toast = "请输入有效的挂单数量") }
            return
        }
        val bal = s.balances[s.fromBookId] ?: 0
        if (from > bal) {
            _uiState.update { it.copy(toast = "《${s.fromBookId}》书币余额不足（可用 ${bal}）") }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.placeExchangeOrder(s.fromBookId, s.toBookId, from, to)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "挂单成功，已锁仓 ${from} 枚", fromAmount = "", toAmount = "") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapExchangeErr(e)) } }
            loadAll()
        }
    }

    fun fillOrder(orderId: String) {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.fillExchangeOrder(orderId)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "吃单成功，兑换完成") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapExchangeErr(e)) } }
            loadAll()
        }
    }

    fun cancelOrder(orderId: String) {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.cancelExchangeOrder(orderId)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "已撤单，锁仓退回") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapExchangeErr(e)) } }
            loadAll()
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }

    private fun mapExchangeErr(e: Throwable): String {
        if (e is HttpException) {
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val code = body?.let { Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
            return when (e.code()) {
                401 -> "请先登录"
                403 -> "无权限（仅本人可撤单）"
                400 -> code?.let { translateExchangeErr(it) } ?: "请求参数有误"
                else -> code?.let { translateExchangeErr(it) } ?: "操作失败（${e.code()}）"
            }
        }
        return "操作失败，后端未连接"
    }

    private fun translateExchangeErr(code: String): String = when (code) {
        "same_book" -> "不能与自己换"
        "invalid_amount" -> "数量无效"
        "insufficient_coins" -> "书币余额不足"
        "order_closed" -> "该订单已关闭"
        "own_order" -> "不能吃自己的单"
        "maker_locked_short" -> "挂单方锁仓不足"
        "bookId required" -> "缺少书 ID"
        else -> "操作失败：$code"
    }
}
