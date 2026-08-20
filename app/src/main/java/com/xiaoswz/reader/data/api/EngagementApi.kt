package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 月票 / 评分 / 评论 / 章评 / 段评。从 BackendApi.kt 拆分（P2）。
interface EngagementApi {

    /** 投月票/推荐票 */
    @POST("api/votes")
    suspend fun castVote(@Body body: VoteBody): VoteResult

    /** 我的剩余票数 */
    @GET("api/votes")
    suspend fun getVoteBalance(): VoteBalance

    /** 提交/修改评分 */
    @POST("api/ratings")
    suspend fun submitRating(@Body body: RatingBody): OkAck

    /** 平均分 + 我的评分 */
    @GET("api/ratings")
    suspend fun getRating(
        @Query("bookSourceId") src: String,
        @Query("bookId") id: String,
    ): RatingResponse

    /** 评论列表 */
    @GET("api/books/{src}/{id}/comments")
    suspend fun getComments(
        @Path("src") src: String,
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): CommentListResponse

    /** 发评论 / 楼中楼 */
    @POST("api/books/{src}/{id}/comments")
    suspend fun postComment(
        @Path("src") src: String,
        @Path("id") id: String,
        @Body body: CommentBody,
    ): OkAck

    /** 评论点赞 */
    @POST("api/comments/{id}/like")
    suspend fun likeComment(@Path("id") id: String): OkAck

    /** 评论举报（基础风控，自增 reportCount，审核后台预留） */
    @POST("api/comments/{id}/report")
    suspend fun reportComment(@Path("id") id: String): OkAck

    /** 章评列表（锚定到章节） */
    @GET("api/books/{src}/{id}/chapters/{chapterId}/comments")
    suspend fun getChapterComments(
        @Path("src") src: String,
        @Path("id") id: String,
        @Path("chapterId") chapterId: String,
        @Query("page") page: Int = 1,
    ): CommentListResponse

    /** 发章评 */
    @POST("api/books/{src}/{id}/chapters/{chapterId}/comments")
    suspend fun postChapterComment(
        @Path("src") src: String,
        @Path("id") id: String,
        @Path("chapterId") chapterId: String,
        @Body body: CommentBody,
    ): OkAck

    /** 段评列表（按章节一次性拉取，客户端按 paragraphIndex 分组） */
    @GET("api/books/{src}/{id}/chapters/{chapterId}/segments")
    suspend fun getSegmentComments(
        @Path("src") src: String,
        @Path("id") id: String,
        @Path("chapterId") chapterId: String,
    ): SegmentCommentListResponse

    /** 发段评（锚定到段落 + 段内偏移 + 引用快照） */
    @POST("api/books/{src}/{id}/chapters/{chapterId}/segments")
    suspend fun postSegmentComment(
        @Path("src") src: String,
        @Path("id") id: String,
        @Path("chapterId") chapterId: String,
        @Body body: SegmentCommentBody,
    ): SegmentCommentItem

    /** 全书段评聚合（v0.15.3 段评独立全屏页）：跨章节根段评，按时间倒序，带 replyCount，cursor 分页 */
    @GET("api/books/{src}/{id}/segment-comments")
    suspend fun getBookSegmentComments(
        @Path("src") src: String,
        @Path("id") id: String,
        @Query("cursor") cursor: Long? = null,
    ): BookSegmentCommentListResponse
}
