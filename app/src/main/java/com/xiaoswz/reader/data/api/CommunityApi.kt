package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 书友圈（社区）：动态流 / 话题 / 发帖评论点赞 / 热门榜 / 举报。从 BackendApi.kt 拆分（P2）。
interface CommunityApi {

    /** 动态流：广场 / 关注 / 关键词搜索 / 话题筛选（page 分页）。匿名可浏览广场 */
    @GET("api/community/posts")
    suspend fun getPosts(
        @Query("feed") feed: String = "square",
        @Query("page") page: Int = 1,
        @Query("topic") topic: String? = null,
        @Query("keyword") keyword: String? = null,
    ): PostListResponse

    /** 话题标签列表（书友圈话题筛选用） */
    @GET("api/community/topics")
    suspend fun getTopics(): TopicListResponse

    /** 作者编辑自己的动态（content / imageUrls / topicId）。管理员亦可。 */
    @PATCH("api/community/posts/{id}")
    suspend fun updatePost(
        @Path("id") id: String,
        @Body body: PostUpdateBody,
    ): OkAck

    /** 作者删除自己的动态（软删）。管理员亦可。 */
    @DELETE("api/community/posts/{id}")
    suspend fun deleteOwnPost(@Path("id") id: String): OkAck

    /** 发帖（需登录；content 必填，imageUrls 仅存 http(s) URL，最多 9 张） */
    @POST("api/community/posts")
    suspend fun createPost(@Body body: PostCreateBody): PostCreateResponse

    /** 单帖详情（含评论）。作者/管理员可删 */
    @GET("api/community/posts/{id}")
    suspend fun getPostDetail(@Path("id") id: String): PostDetail

    /** 动态点赞 / 取消（去重切换，需登录） */
    @POST("api/community/posts/{id}/like")
    suspend fun likePost(@Path("id") id: String): LikeResponse

    /** 动态评论列表（page 分页） */
    @GET("api/community/posts/{id}/comments")
    suspend fun getPostComments(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): PostCommentListResponse

    /** 发评论 / 楼中楼（需登录） */
    @POST("api/community/posts/{id}/comments")
    suspend fun commentPost(
        @Path("id") id: String,
        @Body body: PostCommentBody,
    ): OkAck

    /** 热门动态 */
    @GET("api/community/hot-posts")
    suspend fun getHotPosts(): HotPostsResponse

    /** 热门书单 */
    @GET("api/community/hot-booklists")
    suspend fun getHotBooklists(): HotBooklistsResponse

    /** 动态举报 */
    @POST("api/community/posts/{id}/report")
    suspend fun reportPost(
        @Path("id") id: String,
        @Body body: ReportBody,
    ): OkAck
}
