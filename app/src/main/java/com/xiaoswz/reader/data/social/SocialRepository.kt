package com.xiaoswz.reader.data.social

import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.BooklistListResponse
import com.xiaoswz.reader.data.api.HotPostsResponse
import com.xiaoswz.reader.data.api.PostListResponse
import com.xiaoswz.reader.data.api.ReadingSessionBody
import com.xiaoswz.reader.data.api.ReadingStats
import com.xiaoswz.reader.data.api.ReportBody
import com.xiaoswz.reader.data.api.UserBookshelfResponse
import com.xiaoswz.reader.data.api.UserListResponse
import com.xiaoswz.reader.data.api.UserProfile

/**
 * 用户主页 & 互动（0.7.5）、激励（0.7.7）、内容治理（0.7.8）。统一 Result<T> 包裹。
 */
object SocialRepository {
    private fun <T> mapError(e: Throwable): Result<T> {
        val msg = e.message ?: ""
        return when {
            msg.contains("login_required") -> Result.failure(Exception("请先登录后再操作"))
            msg.contains("forbidden") -> Result.failure(Exception("无权操作"))
            msg.contains("user not found") -> Result.failure(Exception("用户不存在"))
            else -> Result.failure(e)
        }
    }

    suspend fun getProfile(id: String): Result<UserProfile> = runCatching {
        BackendClient.api.getUserProfile(id)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getUserPosts(id: String, page: Int): Result<PostListResponse> = runCatching {
        BackendClient.api.getUserPosts(id, page)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getUserBooklists(id: String, page: Int): Result<BooklistListResponse> = runCatching {
        BackendClient.api.getUserBooklists(id, page)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getUserBookshelf(id: String): Result<UserBookshelfResponse> = runCatching {
        BackendClient.api.getUserBookshelf(id)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getFollowers(id: String, page: Int): Result<UserListResponse> = runCatching {
        BackendClient.api.getFollowers(id, page)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getFollowing(id: String, page: Int): Result<UserListResponse> = runCatching {
        BackendClient.api.getFollowing(id, page)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun follow(id: String): Result<Pair<Boolean, Int>> = runCatching {
        val r = BackendClient.api.followUser(id)
        r.following to r.followerCount
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun block(id: String): Result<Boolean> = runCatching {
        BackendClient.api.blockUser(id).blocked
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getHotPosts(): Result<HotPostsResponse> = runCatching {
        BackendClient.api.getHotPosts()
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun postReadingSession(bookSourceId: String, bookId: String, durationSec: Int): Result<ReadingStats> =
        runCatching {
            BackendClient.api.postReadingSession(
                ReadingSessionBody(bookSourceId = bookSourceId, bookId = bookId, durationSec = durationSec),
            )
        }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getReadingStats(): Result<ReadingStats> = runCatching {
        BackendClient.api.getReadingStats()
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun reportPost(id: String, reason: String?): Result<Boolean> = runCatching {
        BackendClient.api.reportPost(id, ReportBody(reason)).ok
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun reportBooklist(id: String, reason: String?): Result<Boolean> = runCatching {
        BackendClient.api.reportBooklist(id, ReportBody(reason)).ok
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })
}
