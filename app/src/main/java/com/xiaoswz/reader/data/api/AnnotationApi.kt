package com.xiaoswz.reader.data.api

import com.xiaoswz.reader.data.annotation.AnnotationListResponse
import com.xiaoswz.reader.data.annotation.AnnotationPushBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 标注 / 书签（阅读痕迹，跨设备同步）。从 BackendApi.kt 拆分（P2）。
interface AnnotationApi {

    @GET("api/annotations")
    suspend fun getAnnotations(@Query("bookId") bookId: String): AnnotationListResponse

    @POST("api/annotations")
    suspend fun pushAnnotations(@Body body: AnnotationPushBody): AnnotationPushAck

    @DELETE("api/annotations/{clientId}")
    suspend fun deleteAnnotation(@Path("clientId") clientId: String): OkAck
}
