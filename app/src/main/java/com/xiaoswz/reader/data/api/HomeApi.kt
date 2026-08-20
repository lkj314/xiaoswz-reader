package com.xiaoswz.reader.data.api

import retrofit2.http.GET

// 运营位（首页 banner / 精选书单 / 公告）。从 BackendApi.kt 拆分（P2）。
interface HomeApi {

    @GET("api/home")
    suspend fun getHome(): HomeResponse
}
