package com.xiaoswz.reader.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.PostItem
import com.xiaoswz.reader.data.api.PostTopic
import com.xiaoswz.reader.data.community.CommunityRepository
import com.xiaoswz.reader.data.social.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityUiState(
    val feed: String = "square", // square | following | hot
    val topics: List<PostTopic> = emptyList(),
    val selectedTopicId: String? = null,
    val keyword: String? = null,
    val accountId: String? = null,
    val posts: List<PostItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val totalPages: Int = 1,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages
}

class CommunityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadTopics()
    }

    /** 刷新第一页（切换流 / 下拉刷新 / 发帖后调用） */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchPage(1)
    }

    /** 拉取话题标签（话题筛选用） */
    fun loadTopics() {
        viewModelScope.launch {
            CommunityRepository.getTopics()
                .onSuccess { resp -> _uiState.update { it.copy(topics = resp.topics) } }
                .onFailure { /* 话题为非关键功能，失败静默 */ }
        }
    }

    /** 设置当前登录账号 id（owner 判别） */
    fun setAccountId(id: String?) {
        _uiState.update { it.copy(accountId = id) }
    }

    /** 选择 / 取消话题筛选（重置并重新加载） */
    fun selectTopic(topicId: String?) {
        if (_uiState.value.selectedTopicId == topicId) return
        _uiState.update { it.copy(selectedTopicId = topicId, posts = emptyList(), page = 1, totalPages = 1) }
        refresh()
    }

    /** 关键词搜索（空字符串 = 清除搜索） */
    fun setKeyword(kw: String?) {
        val trimmed = kw?.trim()?.takeIf { it.isNotEmpty() }
        if (_uiState.value.keyword == trimmed) return
        _uiState.update { it.copy(keyword = trimmed, posts = emptyList(), page = 1, totalPages = 1) }
        refresh()
    }

    /** 加载下一页 */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        fetchPage(state.page + 1, append = true)
    }

    /** 切换广场 / 关注流（会重置并重新加载） */
    fun switchFeed(feed: String) {
        if (_uiState.value.feed == feed) return
        _uiState.update { it.copy(feed = feed, posts = emptyList(), page = 1, totalPages = 1, error = null) }
        refresh()
    }

    private fun fetchPage(page: Int, append: Boolean = false) {
        val state = _uiState.value
        val feed = state.feed
        val topicId = state.selectedTopicId
        val keyword = state.keyword
        viewModelScope.launch {
            if (feed == "hot") {
                // 热门榜（0.7.7）：拉热门动态，无分页
                SocialRepository.getHotPosts()
                    .onSuccess { resp ->
                        _uiState.update { st ->
                            st.copy(posts = resp.posts, page = 1, totalPages = 1, isLoading = false, isLoadingMore = false)
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = if (it.posts.isEmpty()) e.message ?: "加载失败" else null) }
                    }
                return@launch
            }
            CommunityRepository.getPosts(feed, page, topicId, keyword)
                .onSuccess { resp ->
                    val pageSize = resp.pageSize.coerceAtLeast(1)
                    val totalPages = ((resp.total + pageSize - 1) / pageSize).coerceAtLeast(1)
                    _uiState.update { st ->
                        st.copy(
                            posts = if (append) st.posts + resp.posts else resp.posts,
                            page = resp.page,
                            totalPages = totalPages,
                            isLoading = false,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = if (it.posts.isEmpty()) e.message ?: "加载失败" else null,
                        )
                    }
                }
        }
    }

    /**
     * 点赞 / 取消点赞（乐观更新列表项）。返回最新 {liked, likeCount} 供详情页同步。
     */
    fun toggleLike(postId: String, onResult: (Boolean, Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            CommunityRepository.likePost(postId)
                .onSuccess { resp ->
                    _uiState.update { st ->
                        st.copy(
                            posts = st.posts.map { p ->
                                if (p.id == postId) {
                                    p.copy(liked = resp.liked, likeCount = resp.likeCount)
                                } else {
                                    p
                                }
                            },
                        )
                    }
                    onResult(resp.liked, resp.likeCount)
                }
                .onFailure { onResult(false, -1) }
        }
    }

    /** 把详情页点赞结果同步回列表项 */
    fun applyLikeToPost(postId: String, liked: Boolean, likeCount: Int) {
        _uiState.update { st ->
            st.copy(
                posts = st.posts.map { p ->
                    if (p.id == postId) p.copy(liked = liked, likeCount = likeCount) else p
                },
            )
        }
    }

    /** 发帖成功后刷新流 */
    fun onPostPublished() {
        refresh()
    }

    /** 管理台：删除帖子后从当前列表移除（详情页也会关闭） */
    fun removePost(postId: String) {
        _uiState.update { st -> st.copy(posts = st.posts.filter { it.id != postId }) }
    }
}
