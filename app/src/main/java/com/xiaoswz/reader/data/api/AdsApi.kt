package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 广告位 / 归因。从 BackendApi.kt 拆分（P2）。
interface AdsApi {

    /** 拉取广告位创意 */
    @GET("api/ads")
    suspend fun getAds(@Query("slot") slot: String): AdResponse

    /** 曝光上报 */
    @POST("api/ads/impressions")
    suspend fun reportImpression(@Body body: AdImpressionBody): OkAck

    /** 点击上报 */
    @POST("api/ads/clicks")
    suspend fun reportClick(@Body body: AdClickBody): OkAck

    /** 来源归因 */
    @POST("api/attribution")
    suspend fun reportAttribution(@Body body: AttributionBody): OkAck
}
