package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 书籍角色互动（比心 / 标签墙 / 角色讨论）。从 BackendApi.kt 拆分（P2）。
interface CharacterApi {

    /** 书籍角色列表（横滑卡片） */
    @GET("api/books/{src}/{id}/characters")
    suspend fun getCharacters(
        @Path("src") src: String,
        @Path("id") id: String,
    ): CharacterListResponse

    /** 角色详情：比心数 + 我的比心 + 标签墙 */
    @GET("api/characters/{charId}")
    suspend fun getCharacterDetail(@Path("charId") charId: String): CharacterDetailResponse

    /** 比心切换 */
    @POST("api/characters/{charId}/heart")
    suspend fun toggleHeart(@Path("charId") charId: String): HeartResponse

    /** 角色标签列表 */
    @GET("api/characters/{charId}/tags")
    suspend fun getTags(@Path("charId") charId: String): TagListResponse

    /** 新建角色标签 */
    @POST("api/characters/{charId}/tags")
    suspend fun createTag(
        @Path("charId") charId: String,
        @Body body: TagBody,
    ): TagResponse

    /** 标签投票切换 */
    @POST("api/characters/tags/{tagId}/vote")
    suspend fun toggleTagVote(@Path("tagId") tagId: String): TagVoteResponse

    /** 角色讨论列表（复用 Comment 表） */
    @GET("api/characters/{charId}/comments")
    suspend fun getCharacterComments(
        @Path("charId") charId: String,
        @Query("page") page: Int = 1,
    ): CommentListResponse

    /** 发角色讨论 */
    @POST("api/characters/{charId}/comments")
    suspend fun postCharacterComment(
        @Path("charId") charId: String,
        @Body body: CommentBody,
    ): OkAck
}
