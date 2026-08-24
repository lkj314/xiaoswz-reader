package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BookCircleDto
import com.xiaoswz.reader.data.api.CircleRankItem
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 书圈经济体（0.17.0）读者端核心：每书一个书圈。
 * 功能：加入书圈（设定每书独立人设）、竞拍圈主、书币投资、查看排行与我的投资、圈主精选。
 * 书币不可交易不可提现（避 QunQun 投机坑）；所有系数由后端 AppConfig 热调。
 */
data class BookCircleUiState(
    val bookId: String = "",
    val bookTitle: String? = null,
    val circle: BookCircleDto? = null,
    val rank: List<CircleRankItem> = emptyList(),
    val balance: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val saving: Boolean = false,
    val showBidDialog: Boolean = false,
    val bidAmount: String = "",
    val showInvestDialog: Boolean = false,
    val investAmount: String = "",
    val showIdentityDialog: Boolean = false,
    val identityName: String = "",
    val showFeatureDialog: Boolean = false,
    val featureCommentId: String = "",
)

class BookCircleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BookCircleUiState())
    val uiState: StateFlow<BookCircleUiState> = _uiState.asStateFlow()

    fun init(bookId: String, bookTitle: String?) {
        if (_uiState.value.bookId == bookId && _uiState.value.circle != null) return
        _uiState.update { it.copy(bookId = bookId, bookTitle = bookTitle) }
        load()
    }

    fun load() {
        val bookId = _uiState.value.bookId
        if (bookId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            BackendRepository.getBookCircle(bookId)
                .onSuccess { c ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            circle = c,
                            identityName = c.myMembership?.displayName ?: "",
                            balance = it.balance,
                        )
                    }
                    loadRank()
                    loadBalance()
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, error = "书圈加载失败，后端未连接或网络异常") }
                }
        }
    }

    private fun loadRank() {
        val bookId = _uiState.value.bookId
        viewModelScope.launch {
            BackendRepository.getCircleRank(bookId)
                .onSuccess { resp -> _uiState.update { it.copy(rank = resp.items) } }
                .onFailure { /* 排行非关键，静默 */ }
        }
    }

    private fun loadBalance() {
        viewModelScope.launch {
            BackendRepository.getCoinBalance()
                .onSuccess { resp -> _uiState.update { it.copy(balance = resp.balance) } }
                .onFailure { /* 余额非关键 */ }
        }
    }

    // ── 加入书圈（设定每书独立人设）──
    fun openIdentityDialog() {
        _uiState.update {
            it.copy(
                showIdentityDialog = true,
                identityName = it.circle?.myMembership?.displayName ?: "",
            )
        }
    }

    fun dismissIdentityDialog() = _uiState.update { it.copy(showIdentityDialog = false) }

    fun joinWithIdentity(name: String) {
        val bookId = _uiState.value.bookId
        val n = name.trim().ifEmpty { null }
        _uiState.update { it.copy(saving = true, showIdentityDialog = false) }
        viewModelScope.launch {
            BackendRepository.joinBookCircle(bookId, n, null)
                .onSuccess { c ->
                    _uiState.update { it.copy(saving = false, circle = c, toast = "已加入书圈") }
                    loadRank()
                }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapErr(e)) } }
        }
    }

    // ── 竞拍圈主 ──
    fun openBidDialog() = _uiState.update {
        it.copy(showBidDialog = true, bidAmount = (it.circle?.currentBid?.plus(10) ?: 10).toString())
    }

    fun dismissBidDialog() = _uiState.update { it.copy(showBidDialog = false) }

    fun confirmBid() {
        val bookId = _uiState.value.bookId
        val amt = _uiState.value.bidAmount.toIntOrNull()
        val cur = _uiState.value.circle?.currentBid ?: 0
        if (amt == null || amt <= cur) {
            _uiState.update { it.copy(toast = "出价必须高于当前最高 ${cur} 书币") }
            return
        }
        if (amt > _uiState.value.balance) {
            _uiState.update { it.copy(toast = "书币余额不足（当前 ${_uiState.value.balance}）") }
            return
        }
        _uiState.update { it.copy(saving = true, showBidDialog = false) }
        viewModelScope.launch {
            BackendRepository.bidCircleOwner(bookId, amt)
                .onSuccess { _uiState.update { s -> s.copy(saving = false, toast = "竞拍成功，你已是圈主！") } }
                .onFailure { e ->
                    _uiState.update { it.copy(saving = false, toast = mapErr(e)) }
                }
            // 重新拉取圈状态（圈主可能已变）
            BackendRepository.getBookCircle(bookId).onSuccess { c ->
                _uiState.update { it.copy(circle = c) }
                loadBalance()
            }
        }
    }

    // ── 投资 ──
    fun openInvestDialog() = _uiState.update { it.copy(showInvestDialog = true, investAmount = "") }

    fun dismissInvestDialog() = _uiState.update { it.copy(showInvestDialog = false) }

    fun confirmInvest() {
        val bookId = _uiState.value.bookId
        val amt = _uiState.value.investAmount.toIntOrNull()
        val cap = _uiState.value.circle?.investMaxPerBook ?: 5000
        if (amt == null || amt <= 0) {
            _uiState.update { it.copy(toast = "请输入有效的书币数量") }
            return
        }
        if (amt > _uiState.value.balance) {
            _uiState.update { it.copy(toast = "书币余额不足（当前 ${_uiState.value.balance}）") }
            return
        }
        if (amt > cap) {
            _uiState.update { it.copy(toast = "单书投资上限 ${cap} 书币") }
            return
        }
        _uiState.update { it.copy(saving = true, showInvestDialog = false) }
        viewModelScope.launch {
            BackendRepository.investBook(bookId, amt)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "投资成功，已锁定份额") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapErr(e)) } }
            BackendRepository.getBookCircle(bookId).onSuccess { c ->
                _uiState.update { it.copy(circle = c) }
                loadBalance()
            }
        }
    }

    // ── 圈主精选（仅圈主）──
    fun openFeatureDialog() = _uiState.update { it.copy(showFeatureDialog = true, featureCommentId = "") }

    fun dismissFeatureDialog() = _uiState.update { it.copy(showFeatureDialog = false) }

    fun confirmFeature() {
        val bookId = _uiState.value.bookId
        val cid = _uiState.value.featureCommentId.trim()
        if (cid.isEmpty()) {
            _uiState.update { it.copy(toast = "请输入要精选的评论 ID") }
            return
        }
        _uiState.update { it.copy(saving = true, showFeatureDialog = false) }
        viewModelScope.launch {
            BackendRepository.featureComment(bookId, cid)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "已精选该评论") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapErr(e)) } }
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }

    // 对话框输入（StateFlow 只读，统一经 setter 更新）
    fun setBidAmount(v: String) = _uiState.update { it.copy(bidAmount = v) }
    fun setInvestAmount(v: String) = _uiState.update { it.copy(investAmount = v) }
    fun setIdentityName(v: String) = _uiState.update { it.copy(identityName = v) }
    fun setFeatureCommentId(v: String) = _uiState.update { it.copy(featureCommentId = v) }

    private fun mapErr(e: Throwable): String = when (e) {
        is HttpException -> when (e.code()) {
            403 -> "无权限（请用管理员账号操作）"
            401 -> "登录已失效，请重新登录"
            400 -> "请求参数有误"
            else -> "操作失败（${e.code()}）"
        }
        else -> "操作失败，后端未连接"
    }
}
