package com.xiaoswz.reader.data.api

import retrofit2.http.GET
import retrofit2.http.Query

// 自有书库（catalog）：读冲浪阅读自有库，不实时爬主站。从 BackendApi.kt 拆分（P2）。
interface CatalogApi {

    @GET("api/catalog")
    suspend fun getCatalog(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("sort") sort: String = "latest",
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
    ): CatalogListResponse

    /** 书库分区（分类）列表：用于「分区浏览」入口 */
    @GET("api/catalog/categories")
    suspend fun getCatalogCategories(): CatalogCategoriesResponse
}
