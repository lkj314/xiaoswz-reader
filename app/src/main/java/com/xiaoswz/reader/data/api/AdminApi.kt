package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 管理台（admin，App 内嵌模块）：书籍 / 角色 / 公告治理 + 书友圈内容硬删。
// 从 BackendApi.kt 拆分（P2）。
interface AdminApi {

    /** 管理台：书籍列表（q 书名过滤） */
    @GET("api/admin/books")
    suspend fun adminListBooks(
        @Query("q") q: String?,
        @Query("page") page: Int,
    ): AdminBookListResponse

    /** 管理台：书籍元数据编辑（书名/作者/封面/隐藏） */
    @PATCH("api/admin/books/{src}/{id}")
    suspend fun adminPatchBook(
        @Path("src") src: String,
        @Path("id") id: String,
        @Body body: AdminBookPatchBody,
    ): AdminBookPatchResponse

    /** 管理台：某书角色列表（按 bookId 过滤） */
    @GET("api/admin/characters")
    suspend fun adminListCharacters(@Query("bookId") bookId: String): AdminCharacterListResponse

    /** 管理台：角色录入 / 更新（同名则更新） */
    @POST("api/admin/characters")
    suspend fun adminUpsertCharacter(@Body body: AdminCharacterUpsertBody): AdminCharacterUpsertResponse

    /** 管理台：删除角色 */
    @DELETE("api/admin/characters/{id}")
    suspend fun adminDeleteCharacter(@Path("id") id: String): OkAck

    /** 管理台：公告列表 */
    @GET("api/admin/announcements")
    suspend fun adminListAnnouncements(): AdminAnnouncementListResponse

    /** 管理台：新建公告 */
    @POST("api/admin/announcements")
    suspend fun adminCreateAnnouncement(@Body body: AdminAnnouncementBody): AdminAnnouncementResponse

    /** 管理台：编辑公告 */
    @PATCH("api/admin/announcements/{id}")
    suspend fun adminPatchAnnouncement(
        @Path("id") id: String,
        @Body body: AdminAnnouncementBody,
    ): AdminAnnouncementResponse

    /** 管理台：删除公告 */
    @DELETE("api/admin/announcements/{id}")
    suspend fun adminDeleteAnnouncement(@Path("id") id: String): OkAck

    /** 管理台：删除动态（硬删，级联评论与楼中楼）。仅 admin。 */
    @DELETE("api/admin/posts/{id}")
    suspend fun adminDeletePost(@Path("id") id: String): OkAck

    /** 管理台：删除动态评论（硬删，级联楼中楼；重算帖子评论数）。仅 admin。 */
    @DELETE("api/admin/community/posts/{postId}/comments/{commentId}")
    suspend fun adminDeletePostComment(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String,
    ): OkAck

    /** 管理台：新建作者日志（仅 admin） */
    @POST("api/admin/author-logs")
    suspend fun adminCreateAuthorLog(@Body body: AuthorLogCreateBody): AuthorLogResponse

    /** 管理台：编辑作者日志（仅 admin） */
    @PATCH("api/admin/author-logs/{id}")
    suspend fun adminPatchAuthorLog(
        @Path("id") id: String,
        @Body body: AuthorLogPatchBody,
    ): AuthorLogResponse

    /** 管理台：删除作者日志（仅 admin） */
    @DELETE("api/admin/author-logs/{id}")
    suspend fun adminDeleteAuthorLog(@Path("id") id: String): OkAck
}
