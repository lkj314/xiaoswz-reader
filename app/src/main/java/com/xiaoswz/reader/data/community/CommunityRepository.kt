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
import com.xiaoswz.reader.data.api.PostUpdateBody
import com.xiaoswz.reader.data.api.TopicListResponse
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

    /** 动态流（square / following / 话题筛选 / 关键词搜索） */
    suspend fun getPosts(
        feed: String,
        page: Int,
        topicId: String? = null,
        keyword: String? = null,
    ): Result<PostListResponse> =
        runCommunity { BackendClient.api.getPosts(feed, page, topicId, keyword) }

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

    /** 管理台：删除动态（硬删，级联评论与楼中楼）。仅 admin 账号可调。 */
    suspend fun deletePost(id: String): Result<OkAck> =
        runCommunity { BackendClient.api.adminDeletePost(id) }

    /** 管理台：删除动态评论（硬删，级联楼中楼；重算帖子评论数）。仅 admin 账号可调。 */
    suspend fun deletePostComment(postId: String, commentId: String): Result<OkAck> =
        runCommunity { BackendClient.api.adminDeletePostComment(postId, commentId) }

    /** 话题标签列表（话题筛选用） */
    suspend fun getTopics(): Result<TopicListResponse> =
        runCommunity { BackendClient.api.getTopics() }

    /** 作者编辑自己的动态（自改闭环）。管理员亦可。 */
    suspend fun editPost(
        id: String,
        content: String? = null,
        imageUrls: List<String>? = null,
        topicId: String? = null,
    ): Result<OkAck> = runCommunity {
        BackendClient.api.updatePost(id, PostUpdateBody(content = content, imageUrls = imageUrls, topicId = topicId))
    }

    /** 作者删除自己的动态（自删闭环）。管理员亦可。 */
    suspend fun deleteOwnPost(id: String): Result<OkAck> =
        runCommunity { BackendClient.api.deleteOwnPost(id) }

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
            "forbidden" -> Exception("无权限（需管理员账号）")
            "post not found", "not_found" -> Exception("内容不存在或已被删除")
            else -> Exception("操作失败（错误 ${e.code()}）")
        }
    }
}
