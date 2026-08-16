package com.xiaoswz.reader.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.PostItem
import com.xiaoswz.reader.data.community.CommunityRepository
import com.xiaoswz.reader.data.social.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityUiState(
    val feed: String = "square", // square | following | hot
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
    }

    /** 刷新第一页（切换流 / 下拉刷新 / 发帖后调用） */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchPage(1)
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
        val feed = _uiState.value.feed
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
            CommunityRepository.getPosts(feed, page)
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
}
