package com.xiaoswz.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.model.BookDetailDto
import com.xiaoswz.reader.data.model.resolveCoverUrl
import com.xiaoswz.reader.data.api.AdCreativeDto
import com.xiaoswz.reader.data.api.BookStatsResponse
import com.xiaoswz.reader.data.api.CommentItem
import com.xiaoswz.reader.data.api.RatingResponse
import com.xiaoswz.reader.data.api.VoteBalance
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DetailUiState(
    val detail: BookDetailDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // ── 冲浪阅读独立后端互动数据（P1–P3）──
    val stats: BookStatsResponse? = null,
    val voteBalance: VoteBalance? = null,
    val rating: RatingResponse? = null,
    val comments: List<CommentItem> = emptyList(),
    val commentTotal: Int = 0,
    val ad: AdCreativeDto? = null,
    val toast: String? = null,
    // ── 整本离线下载（0.9.5）──
    val isDownloading: Boolean = false,
    val downloadDone: Int = 0,
    val downloadTotal: Int = 0,
)

class BookDetailViewModel(
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(slug: String) {
        if (_uiState.value.detail?.id == slug) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getBookDetail(slug)
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                    loadBackend(slug, detail)
                    // 进书即热：后台预热前 2 章，点开阅读第一眼即零等待
                    detail.chapters?.take(2)?.forEach { ch ->
                        val cid = ch.id ?: return@forEach
                        viewModelScope.launch { runCatching { repository.prefetchChapter(cid) } }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "加载失败，请稍后重试")
                    }
                }
        }
    }

    /** 拉取后端互动数据 + 上报浏览（后端不可达时静默失败，不阻塞阅读） */
    private fun loadBackend(slug: String, detail: BookDetailDto) {
        viewModelScope.launch {
            // 上报浏览：先用 resolveCoverUrl 把书源返回的相对/协议相对封面解析成绝对
            // http 地址再上报，否则 book_stats.coverUrl 永远为 null → 排行榜看不到封面。
            // 遵守"封面绝不存 data: URI"铁律：data: 经 resolveCoverUrl 会返回 ByteBuffer
            // （非 String），这里 as? String 自动过滤掉，不会误存。
            val resolvedCover = resolveCoverUrl(detail.coverUrl)
            val cover = (resolvedCover as? String)?.takeIf { it.startsWith("http") }
            BackendRepository.reportBookView(slug, detail.name, detail.author, cover)

            // 并行拉取展示数据
            val stats = BackendRepository.getBookStats(slug).getOrNull()
            val balance = BackendRepository.getVoteBalance().getOrNull()
            val rating = BackendRepository.getRating(slug).getOrNull()
            val comments = BackendRepository.getComments(slug).getOrNull()
            val ad = BackendRepository.getAds("detail").getOrNull()?.creatives?.firstOrNull()

            _uiState.update {
                it.copy(
                    stats = stats,
                    voteBalance = balance,
                    rating = rating,
                    comments = comments?.comments ?: emptyList(),
                    commentTotal = comments?.total ?: 0,
                    ad = ad,
                )
            }
            // 广告曝光上报（fire-and-forget）
            ad?.id?.let { id -> BackendRepository.reportImpression(id, "detail") }
        }
    }

    fun vote() {
        val slug = _uiState.value.detail?.id ?: return
        viewModelScope.launch {
            val res = BackendRepository.castVote(slug, "monthly")
            if (res.isSuccess && res.getOrNull()?.ok == true) {
                _uiState.update { s ->
                    s.copy(
                        toast = "月票 +1（今日剩余 ${res.getOrNull()?.remaining ?: 0}）",
                        voteBalance = res.getOrNull()?.remaining?.let { r ->
                            (s.voteBalance ?: VoteBalance()).copy(used = (s.voteBalance?.used ?: 0) + 1, remaining = r)
                        } ?: s.voteBalance,
                        stats = s.stats?.copy(voteMonth = s.stats.voteMonth + 1, voteCount = s.stats.voteCount + 1),
                    )
                }
            } else {
                _uiState.update { it.copy(toast = "投票失败：今日票数已用完或后端未连接") }
            }
        }
    }

    fun submitRating(score: Int) {
        val slug = _uiState.value.detail?.id ?: return
        viewModelScope.launch {
            val res = BackendRepository.submitRating(slug, score)
            if (res.isSuccess) {
                val rating = BackendRepository.getRating(slug).getOrNull()
                val stats = BackendRepository.getBookStats(slug).getOrNull()
                _uiState.update { it.copy(rating = rating, stats = stats, toast = "评分成功：$score 星") }
            } else {
                _uiState.update { it.copy(toast = "评分失败，后端未连接") }
            }
        }
    }

    fun postComment(content: String) {
        val slug = _uiState.value.detail?.id ?: return
        val text = content.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            // 登录门禁：评论仅对 user / admin 开放；游客引导登录，禁言用户拦截
            val appSettings = AppSettingsRepository(AppContext.app)
            when (appSettings.getAccountRole()) {
                "guest" -> {
                    _uiState.update { it.copy(toast = "请先登录后再评论") }
                    return@launch
                }
            }
            if (appSettings.isMuted()) {
                _uiState.update { it.copy(toast = "您已被禁言，暂时无法评论") }
                return@launch
            }
            val res = BackendRepository.postComment(slug, text)
            if (res.isSuccess) {
                val comments = BackendRepository.getComments(slug).getOrNull()
                val stats = BackendRepository.getBookStats(slug).getOrNull()
                _uiState.update {
                    it.copy(
                        comments = comments?.comments ?: emptyList(),
                        commentTotal = comments?.total ?: 0,
                        stats = stats,
                        toast = "评论已发布",
                    )
                }
            } else {
                // 区分后端明确拒绝（403 禁言 / 登录失效）与网络不可达
                val msg = when (val t = res.exceptionOrNull()) {
                    is retrofit2.HttpException -> when (t.code()) {
                        403 -> "您已被禁言，暂时无法评论"
                        401 -> "登录已失效，请重新登录"
                        else -> "评论失败，请稍后重试"
                    }
                    else -> "评论失败，后端未连接"
                }
                _uiState.update { it.copy(toast = msg) }
            }
        }
    }

    fun likeComment(id: String) {
        viewModelScope.launch {
            BackendRepository.likeComment(id)
            // 本地 +1 即时反馈
            _uiState.update { s ->
                s.copy(comments = s.comments.map { if (it.id == id) it.copy(likeCount = it.likeCount + 1) else it })
            }
        }
    }

    fun reportComment(id: String) {
        viewModelScope.launch {
            BackendRepository.reportComment(id)
            _uiState.update { it.copy(toast = "已举报，感谢反馈") }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toast = null) }
    }

    /** 整本离线下载：并发拉取全部章节正文落本地文件缓存，带进度回调（0.9.5） */
    fun downloadWholeBook() {
        val detail = _uiState.value.detail ?: return
        val chapters = detail.chapters.orEmpty()
        if (chapters.isEmpty() || _uiState.value.isDownloading) return
        _uiState.update {
            it.copy(isDownloading = true, downloadDone = 0, downloadTotal = chapters.size)
        }
        viewModelScope.launch {
            repository.downloadWholeBook(chapters) { done, total ->
                _uiState.update { it.copy(downloadDone = done, downloadTotal = total) }
            }
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    toast = "已离线缓存 ${chapters.size} 章，断网可读",
                )
            }
        }
    }

    fun retry(slug: String) {
        _uiState.update { it.copy(detail = null) }
        load(slug)
    }
}
