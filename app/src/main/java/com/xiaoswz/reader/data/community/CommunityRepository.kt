package com.xiaoswz.reader.data.community

import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.LikeResponse
import com.xiaoswz.reader.data.api.OkAck
import com.xiaoswz.reader.data.api.PostCommentBody
import com.xiaoswz.reader.data.api.PostCommentListResponse
import com.xiaoswz.reader.data.api.PostCreateBody
import com.xiaoswz.reader.data.api.PostCreateResponse
import com.xiaoswz.reader.data.api.PostDetail
import com.xiaoswz.reader.data.api.PostListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * 书友圈（社区）数据层：封装 0.7.2 落地的后端社区 API。
 * 鉴权由 [BackendClient] 的 Bearer 拦截器自动注入；本类只负责调用 + 错误归类。
 *
 * 约定（后端返回）：
 *  - 403 {error:"login_required"} → 需登录
 *  - 403 {error:"muted"}         → 账号被禁言
 *  - 广场流匿名可浏览；关注流 / 发帖 / 点赞 / 评论需登录。
 */
object CommunityRepository {

    private suspend inline fun <T> runCommunity(
        crossinline block: suspend () -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: HttpException) {
            Result.failure(mapCommunityError(e))
        } catch (e: IOException) {
            Result.failure(Exception("网络不可达，请检查后端连接"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 动态流（square / following） */
    suspend fun getPosts(feed: String, page: Int): Result<PostListResponse> =
        runCommunity { BackendClient.api.getPosts(feed, page) }

    /** 发帖 */
    suspend fun createPost(
        content: String,
        imageUrls: List<String>,
    ): Result<PostCreateResponse> = runCommunity {
        BackendClient.api.createPost(PostCreateBody(content = content, imageUrls = imageUrls))
    }

    /** 分享书单到书友圈（0.7.4）：发一条引用该书单的动态 */
    suspend fun shareBooklist(booklistId: String, title: String, description: String?): Result<PostCreateResponse> =
        runCommunity {
            BackendClient.api.createPost(
                PostCreateBody(
                    content = "分享书单：$title${description?.let { "\n$it" } ?: ""}",
                    imageUrls = emptyList(),
                    booklistId = booklistId,
                ),
            )
        }

    /** 单帖详情（含评论） */
    suspend fun getPostDetail(id: String): Result<PostDetail> =
        runCommunity { BackendClient.api.getPostDetail(id) }

    /** 点赞 / 取消点赞 */
    suspend fun likePost(id: String): Result<LikeResponse> =
        runCommunity { BackendClient.api.likePost(id) }

    /** 评论列表 */
    suspend fun getComments(id: String, page: Int): Result<PostCommentListResponse> =
        runCommunity { BackendClient.api.getPostComments(id, page) }

    /** 发评论 */
    suspend fun commentPost(id: String, content: String, parentId: String? = null): Result<OkAck> =
        runCommunity { BackendClient.api.commentPost(id, PostCommentBody(content, parentId)) }

    private fun mapCommunityError(e: HttpException): Exception {
        val errCode = try {
            e.response()?.errorBody()?.string()?.let { body ->
                Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            }
        } catch (_: Exception) {
            null
        }
        return when (errCode) {
            "login_required" -> Exception("请先登录后再操作")
            "muted" -> Exception("你已被禁言，暂时无法操作")
            "post not found", "not_found" -> Exception("内容不存在或已被删除")
            else -> Exception("操作失败（错误 ${e.code()}）")
        }
    }
}
