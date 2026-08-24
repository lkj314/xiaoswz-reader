package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 书圈经济体（0.17.0）：书圈主页 / 加入 / 竞拍 / 精选 / 投资 / 排行
interface BookCircleApi {
    @GET("api/book-circle")
    suspend fun getBookCircle(
        @Query("bookId") bookId: String,
    ): BookCircleDto

    @POST("api/book-circle/join")
    suspend fun joinBookCircle(@Body body: CircleJoinBody): BookCircleDto

    @POST("api/book-circle/bid")
    suspend fun bidCircleOwner(@Body body: CircleBidBody): BidResultDto

    @POST("api/book-circle/feature")
    suspend fun featureComment(@Body body: CircleFeatureBody): OkAck

    @POST("api/book-circle/invest")
    suspend fun investBook(@Body body: CircleInvestBody): InvestResultDto

    @GET("api/book-circle/investment")
    suspend fun getInvestment(
        @Query("bookId") bookId: String,
        @Query("page") page: Int = 1,
    ): InvestmentListResponse

    @GET("api/book-circle/rank")
    suspend fun getCircleRank(
        @Query("bookId") bookId: String,
        @Query("date") date: String? = null,
    ): CircleRankResponse
}
