package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 阅读激励 / 统计（0.7.7）。从 BackendApi.kt 拆分（P2）。
interface ReadingApi {

    @POST("api/reading/session")
    suspend fun postReadingSession(@Body body: ReadingSessionBody): ReadingStats

    @GET("api/reading/stats")
    suspend fun getReadingStats(): ReadingStats
}
