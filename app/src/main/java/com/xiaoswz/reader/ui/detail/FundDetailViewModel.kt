package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.FundDetailResponse
import com.xiaoswz.reader.data.api.GrabResponse
import com.xiaoswz.reader.data.api.RedeemResponse
import com.xiaoswz.reader.data.api.SubscribeResponse
import com.xiaoswz.reader.data.api.TransferStableResponse
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * 理财产品详情（0.19）：资产包构成、净值走势、稳定币产出、认购/赎回、
 * 董事挖稳定币（抓取）与稳定币调拨。
 */
data class FundDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val fundId: String = "",
    val fund: FundDetailResponse? = null,
    val isDirector: Boolean = false, // 当前用户是否为该产品所属书圈的董事
    val busy: Boolean = false,
    val subscribeBookId: String = "",
    val subscribeAmount: String = "",
    val redeemShares: String = "",
    val transferSerial: String = "",
    val transferToFund: String = "",
    val showTransfer: Boolean = false,
)

class FundDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FundDetailUiState())
    val uiState: StateFlow<FundDetailUiState> = _uiState.asStateFlow()

    fun init(fundId: String) {
        if (_uiState.value.fundId == fundId && _uiState.value.fund != null) return
        _uiState.update { it.copy(fundId = fundId) }
        load()
    }

    fun load() {
        val fundId = _uiState.value.fundId
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val detail = BackendRepository.getFundDetail(fundId)
            val hub = BackendRepository.getCircleHub()
            detail.onSuccess { f ->
                val isDirector = hub.getOrNull()?.books?.firstOrNull { it.bookId == f.bookId }?.isDirector ?: false
                _uiState.update { it.copy(isLoading = false, fund = f, isDirector = isDirector) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "产品详情加载失败，后端未连接或网络异常") }
            }
        }
    }

    fun setSubscribeBookId(v: String) = _uiState.update { it.copy(subscribeBookId = v) }
    fun setSubscribeAmount(v: String) = _uiState.update { it.copy(subscribeAmount = v) }
    fun setRedeemShares(v: String) = _uiState.update { it.copy(redeemShares = v) }
    fun setTransferSerial(v: String) = _uiState.update { it.copy(transferSerial = v) }
    fun setTransferToFund(v: String) = _uiState.update { it.copy(transferToFund = v) }
    fun toggleTransfer() = _uiState.update { it.copy(showTransfer = !it.showTransfer) }

    fun subscribe() {
        val s = _uiState.value
        val bookId = s.subscribeBookId.trim()
        val amount = s.subscribeAmount.trim().toIntOrNull()
        if (bookId.isEmpty() || amount == null || amount <= 0) {
            _uiState.update { it.copy(toast = "请填写有效的支付书 ID 与数量") }
            return
        }
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            BackendRepository.subscribeFund(s.fundId, bookId, amount)
                .onSuccess { r ->
                    if (r.ok) {
                        _uiState.update {
                            it.copy(
                                busy = false, subscribeBookId = "", subscribeAmount = "",
                                toast = "认购成功，获得 ${fmt(r.shares)} 份（市值 ${fmt(r.myValue)}）",
                            )
                        }
                        load()
                    } else {
                        _uiState.update { it.copy(busy = false, toast = "认购失败") }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(busy = false, toast = "认购失败：${e.message ?: "网络异常"}") } }
        }
    }

    fun redeem() {
        val s = _uiState.value
        val shares = s.redeemShares.trim().toDoubleOrNull()
        if (shares == null || shares <= 0) {
            _uiState.update { it.copy(toast = "请填写有效的赎回份额") }
            return
        }
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            BackendRepository.redeemFund(s.fundId, shares)
                .onSuccess { r ->
                    if (r.ok) {
                        _uiState.update { it.copy(busy = false, redeemShares = "", toast = "赎回成功，回收市值 ${fmt(r.value)}") }
                        load()
                    } else {
                        _uiState.update { it.copy(busy = false, toast = "赎回失败") }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(busy = false, toast = "赎回失败：${e.message ?: "网络异常"}") } }
        }
    }

    fun grab() {
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            BackendRepository.grabStableCoin(_uiState.value.fundId)
                .onSuccess { r ->
                    val msg = if (r.dropped) {
                        "挖出稳定币 #${r.serial}（恒定兑换 ${r.backing ?: 10} 枚 B000001）"
                    } else {
                        "本次未挖到稳定币（成功率 ${"%.3f".format((r.probability ?: 0.0) * 100)}%）：${r.reason ?: ""}"
                    }
                    _uiState.update { it.copy(busy = false, toast = msg) }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(busy = false, toast = "抓取失败：${e.message ?: "网络异常"}") } }
        }
    }

    fun transfer() {
        val s = _uiState.value
        val serial = s.transferSerial.trim().toIntOrNull()
        val toFund = s.transferToFund.trim()
        if (serial == null || toFund.isEmpty()) {
            _uiState.update { it.copy(toast = "请填写稳定币序号与目标产品 ID") }
            return
        }
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            BackendRepository.transferStableCoin(serial, toFund)
                .onSuccess { r ->
                    if (r.ok) {
                        _uiState.update {
                            it.copy(
                                busy = false, showTransfer = false,
                                transferSerial = "", transferToFund = "",
                                toast = "稳定币 #$serial 已转入《$toFund》",
                            )
                        }
                        load()
                    } else {
                        _uiState.update { it.copy(busy = false, toast = "转移失败") }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(busy = false, toast = "转移失败：${e.message ?: "网络异常"}") } }
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }
}

private val nf = NumberFormat.getNumberInstance(Locale.CHINA)
fun fmt(v: Double): String = nf.format(v)
