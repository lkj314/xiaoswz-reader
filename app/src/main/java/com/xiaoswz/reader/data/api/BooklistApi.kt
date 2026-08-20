package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// 书单推书（0.7.4）。从 BackendApi.kt 拆分（P2）。
interface BooklistApi {

    @GET("api/booklists")
    suspend fun getBooklists(
        @Query("scope") scope: String = "all",
        @Query("page") page: Int = 1,
    ): BooklistListResponse

    @POST("api/booklists")
    suspend fun createBooklist(@Body body: BooklistCreateBody): BooklistCreateResponse

    @GET("api/booklists/{id}")
    suspend fun getBooklistDetail(@Path("id") id: String): BooklistDetail

    @POST("api/booklists/{id}/items")
    suspend fun addBooklistItem(
        @Path("id") id: String,
        @Body body: BooklistItemBody,
    ): OkAck

    @DELETE("api/booklists/{id}/items/{itemId}")
    suspend fun deleteBooklistItem(
        @Path("id") id: String,
        @Path("itemId") itemId: String,
    ): OkAck

    @POST("api/booklists/{id}/collect")
    suspend fun collectBooklist(@Path("id") id: String): CollectResponse

    /** 编辑书单（标题/简介/封面）。仅 owner/admin。 */
    @PUT("api/booklists/{id}")
    suspend fun updateBooklist(
        @Path("id") id: String,
        @Body body: BooklistUpdateBody,
    ): OkAck

    /** 删除书单（软删 status=hidden）。仅 owner/admin。 */
    @DELETE("api/booklists/{id}")
    suspend fun deleteBooklist(@Path("id") id: String): OkAck

    /** 更新书单项（编辑推荐语 / 调整排序）。仅 owner/admin。 */
    @PATCH("api/booklists/{id}/items/{itemId}")
    suspend fun updateBooklistItem(
        @Path("id") id: String,
        @Path("itemId") itemId: String,
        @Body body: BooklistItemUpdateBody,
    ): OkAck

    /** 书单举报 */
    @POST("api/booklists/{id}/report")
    suspend fun reportBooklist(
        @Path("id") id: String,
        @Body body: ReportBody,
    ): OkAck
}
