package com.xiaoswz.reader.data.api

import retrofit2.http.GET
import retrofit2.http.Query

// 作者日志（0.16.5）：公开列表，读者详情页「作者碎碎念」专区 / 作者日志全屏页拉取。
// 后端不可达时调用方静默降级（不抛 UI 线程）。
interface AuthorLogApi {

    /** 某书的作者日志列表（按 置顶→时间 倒序）。type 可选过滤。 */
    @GET("api/author-logs")
    suspend fun getAuthorLogs(
        @Query("bookId") bookId: String,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
    ): AuthorLogListResponse
}
