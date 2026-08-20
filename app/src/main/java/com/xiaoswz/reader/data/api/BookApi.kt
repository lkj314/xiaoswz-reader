package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// 书库发现 / 聚合指标 / 榜单。从 BackendApi.kt 拆分（P2）。
interface BookApi {

    /** 上报浏览/曝光并回写元数据（打开详情页时调用） */
    @POST("api/books/{src}/{id}")
    suspend fun reportBookView(
        @Path("src") src: String,
        @Path("id") id: String,
        @Body body: BookViewBody,
    ): OkAck

    /** 单书聚合指标（月票/评分/评论数/浏览量） */
    @GET("api/books/{src}/{id}")
    suspend fun getBookStats(
        @Path("src") src: String,
        @Path("id") id: String,
    ): BookStatsResponse

    /** 榜单（popularity / monthly / rating / new） */
    @GET("api/leaderboards/{board}")
    suspend fun getLeaderboard(@Path("board") board: String): LeaderboardResponse
}
