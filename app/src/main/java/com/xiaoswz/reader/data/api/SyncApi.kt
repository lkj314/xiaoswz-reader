package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

// 书架 / 进度 / 全量同步（LWW 冲突裁决）。从 BackendApi.kt 拆分（P2）。
interface SyncApi {

    /** 云端拉取：一次取回该用户全部（或 since 以来变更的）书架与进度 */
    @GET("api/sync")
    suspend fun pullSync(@Query("since") since: Long? = null): SyncResponse

    /** 书架 upsert（带 updatedAt 供后端 LWW 冲突裁决） */
    @POST("api/bookshelf")
    suspend fun upsertBookshelf(@Body body: BookshelfUpsertBody): UpsertAck

    /** 阅读进度 upsert */
    @PUT("api/progress")
    suspend fun upsertProgress(@Body body: ProgressUpsertBody): UpsertAck

    /** 书架删除（本地移除后同步到云端） */
    @DELETE("api/bookshelf")
    suspend fun deleteBookshelf(@Body body: BookIdBody): OkAck
}
