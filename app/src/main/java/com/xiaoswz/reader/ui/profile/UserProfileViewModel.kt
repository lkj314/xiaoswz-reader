package com.xiaoswz.reader.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.data.api.PostItem
import com.xiaoswz.reader.data.api.UserBookshelfItem
import com.xiaoswz.reader.data.api.UserProfile
import com.xiaoswz.reader.data.social.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProfileTab { POSTS, BOOKLISTS, BOOKSHELF }

data class UserProfileUiState(
    val profile: UserProfile? = null,
    val tab: ProfileTab = ProfileTab.POSTS,
    val posts: List<PostItem> = emptyList(),
    val booklists: List<BooklistSummary> = emptyList(),
    val bookshelf: List<UserBookshelfItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class UserProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            SocialRepository.getProfile(userId).onSuccess { p ->
                _uiState.value = _uiState.value.copy(profile = p, isLoading = false)
                loadTab(userId, ProfileTab.POSTS)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun switchTab(userId: String, tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(tab = tab)
        loadTab(userId, tab)
    }

    private fun loadTab(userId: String, tab: ProfileTab) {
        viewModelScope.launch {
            when (tab) {
                ProfileTab.POSTS -> SocialRepository.getUserPosts(userId, 1).onSuccess { r ->
                    _uiState.value = _uiState.value.copy(posts = r.posts)
                }.onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                ProfileTab.BOOKLISTS -> SocialRepository.getUserBooklists(userId, 1).onSuccess { r ->
                    _uiState.value = _uiState.value.copy(booklists = r.booklists)
                }.onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                ProfileTab.BOOKSHELF -> SocialRepository.getUserBookshelf(userId).onSuccess { r ->
                    _uiState.value = _uiState.value.copy(bookshelf = r.items)
                }.onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            }
        }
    }

    fun toggleFollow(userId: String, onResult: (Result<Pair<Boolean, Int>>) -> Unit) {
        viewModelScope.launch {
            SocialRepository.follow(userId).onSuccess { (following, count) ->
                _uiState.value = _uiState.value.copy(
                    profile = _uiState.value.profile?.copy(
                        isFollowing = following,
                        followerCount = count,
                    ),
                )
                onResult(Result.success(following to count))
            }.onFailure { e -> onResult(Result.failure(e)) }
        }
    }

    fun block(userId: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            SocialRepository.block(userId).onSuccess { b -> onResult(Result.success(b)) }
                .onFailure { e -> onResult(Result.failure(e)) }
        }
    }
}
