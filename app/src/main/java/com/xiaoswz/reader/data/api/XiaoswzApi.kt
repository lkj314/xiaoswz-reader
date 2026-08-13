package com.xiaoswz.reader.data.api

import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.data.model.BookDetailDto
import com.xiaoswz.reader.data.model.BookListResponse
import com.xiaoswz.reader.data.model.ChapterContentDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * 冲浪中文网公开 API
 * 全部为只读接口，无需登录
 */
interface XiaoswzApi {

    /** 书城列表：sort=latest(最新) / popular(热门)，search 为搜索关键词 */
    @GET("api/books")
    suspend fun getBooks(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 24,
        @Query("sort") sort: String = "latest",
        @Query("search") search: String? = null,
    ): BookListResponse

    /** 书籍详情（含完整目录），bookId 传书籍 slug */
    @GET("api/book-source")
    suspend fun getBookDetail(
        @Query("action") action: String = "detail",
        @Query("bookId") bookId: String,
    ): BookDetailDto

    /** 章节正文（纯文本，服务端已解压） */
    @GET("api/book-source")
    suspend fun getChapterContent(
        @Query("action") action: String = "content",
        @Query("chapterId") chapterId: String,
    ): ChapterContentDto
}

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val api: XiaoswzApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(XiaoswzApi::class.java)
    }
}
