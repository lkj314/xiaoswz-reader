package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BookCircleDto
import com.xiaoswz.reader.data.api.NewsItemDto
import com.xiaoswz.reader.data.backend.BackendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * 董事会（0.18）：董事长 / 董事权限的模拟金融操作。
 * 含：设定基准书币价（信号）、回购锁仓、储备调拨、发布财报 / 利好利空新闻稿。
 * 所有操作在「守恒护栏」内：绝不新铸，只动价格信号与既有币的锁仓 / 互换 / 储备。
 */
data class BoardUiState(
    val bookId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val saving: Boolean = false,
    val circle: BookCircleDto? = null,
    val news: List<NewsItemDto> = emptyList(),
    val isDirector: Boolean = false,
    // 表单
    val anchorPrice: String = "",
    val buybackAmount: String = "",
    val reserveAssetBookId: String = "",
    val reserveAmount: String = "",
    val newsTitle: String = "",
    val newsBody: String = "",
    val newsSentiment: String = "neutral",
)

class BoardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    fun init(bookId: String) {
        _uiState.update { it.copy(bookId = bookId) }
        load()
    }

    fun load() {
        val bookId = _uiState.value.bookId
        if (bookId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val c = BackendRepository.getBookCircle(bookId)
            c.onSuccess { circle ->
                val role = circle.myMembership?.role ?: "member"
                val dir = role in setOf("owner", "elder", "council")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        circle = circle,
                        isDirector = dir,
                        anchorPrice = if (circle.policyAnchorPrice > 0) circle.policyAnchorPrice.toString() else "1.0",
                    )
                }
            }
            c.onFailure { _uiState.update { it.copy(isLoading = false, error = "书圈加载失败，后端未连接或网络异常") } }
            val n = BackendRepository.getCircleNews(bookId)
            n.onSuccess { resp -> _uiState.update { it.copy(news = resp.news) } }
        }
    }

    fun setAnchorPrice(v: String) = _uiState.update { it.copy(anchorPrice = v) }
    fun setBuybackAmount(v: String) = _uiState.update { it.copy(buybackAmount = v) }
    fun setReserveAssetBookId(v: String) = _uiState.update { it.copy(reserveAssetBookId = v) }
    fun setReserveAmount(v: String) = _uiState.update { it.copy(reserveAmount = v) }
    fun setNewsTitle(v: String) = _uiState.update { it.copy(newsTitle = v) }
    fun setNewsBody(v: String) = _uiState.update { it.copy(newsBody = v) }
    fun setNewsSentiment(v: String) = _uiState.update { it.copy(newsSentiment = v) }

    fun setAnchor() {
        val s = _uiState.value
        val price = s.anchorPrice.toFloatOrNull()
        if (price == null || price <= 0) { _uiState.update { it.copy(toast = "请输入有效的基准价") }; return }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.boardSetAnchor(s.bookId, price)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "已设定基准书币价：$price") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapBoardErr(e)) } }
            load()
        }
    }

    fun buyback() {
        val s = _uiState.value
        val amt = s.buybackAmount.toIntOrNull()
        if (amt == null || amt <= 0) { _uiState.update { it.copy(toast = "请输入有效的回购数量") }; return }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.boardBuyback(s.bookId, amt)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "已回购 ${amt} 枚锁入储备", buybackAmount = "") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapBoardErr(e)) } }
            load()
        }
    }

    fun moveReserve() {
        val s = _uiState.value
        val asset = s.reserveAssetBookId.trim()
        val amt = s.reserveAmount.toIntOrNull()
        if (asset.isEmpty() || amt == null || amt <= 0) { _uiState.update { it.copy(toast = "请输入他书 ID 与数量") }; return }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.boardMoveReserve(s.bookId, asset, amt)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "已用国库 ${amt} 换入《${asset}》储备", reserveAssetBookId = "", reserveAmount = "") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapBoardErr(e)) } }
            load()
        }
    }

    fun publishNews() {
        val s = _uiState.value
        if (s.newsTitle.isBlank()) { _uiState.update { it.copy(toast = "请输入新闻标题") }; return }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            BackendRepository.publishCircleNews(s.bookId, "report", s.newsTitle, s.newsBody, s.newsSentiment)
                .onSuccess { _uiState.update { it.copy(saving = false, toast = "新闻稿已发布", newsTitle = "", newsBody = "") } }
                .onFailure { e -> _uiState.update { it.copy(saving = false, toast = mapBoardErr(e)) } }
            load()
        }
    }

    fun clearToast() = _uiState.update { it.copy(toast = null) }

    private fun mapBoardErr(e: Throwable): String {
        if (e is HttpException) {
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val code = body?.let { Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
            return when (e.code()) {
                401 -> "请先登录"
                403 -> code?.let { translateBoardErr(it) } ?: "无权限（需董事身份）"
                400 -> code?.let { translateBoardErr(it) } ?: "请求参数有误"
                else -> code?.let { translateBoardErr(it) } ?: "操作失败（${e.code()}）"
            }
        }
        return "操作失败，后端未连接"
    }

    private fun translateBoardErr(code: String): String = when (code) {
        "not_director" -> "仅董事长 / 董事可操作"
        "quorum_unmet" -> "董事会决议未达法定占比（需 ${"30"}% 以上董事持股共识）"
        "treasury_short" -> "本书国库余额不足"
        "asset_treasury_short" -> "目标书圈国库不足"
        "invalid_price" -> "价格无效"
        "invalid_amount" -> "数量无效"
        "same_book" -> "不能与自身换"
        "bookId required" -> "缺少书 ID"
        else -> "操作失败：$code"
    }
}
