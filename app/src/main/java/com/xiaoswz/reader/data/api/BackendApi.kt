package com.xiaoswz.reader.data.api

import com.xiaoswz.reader.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 冲浪阅读专属后端 API（独立产品后端，与主站数据库隔离）。
 * 与 [XiaoswzApi]（书源只读接口）完全独立：这里的每个请求都带 x-device-id
 * 请求头，由后端据此识别匿名设备账号。
 *
 * 身份模型：每台设备一个 deviceId（首次启动时生成并持久化），作为无状态凭证。
 * 后端据此解析用户，无需登录、不碰主站 Auth。
 */
interface BackendApi {

    /** 设备匿名登录：确保后端存在该设备对应的用户（首同步前调用一次） */
    @POST("api/auth/anon")
    suspend fun anonLogin(@Body body: DeviceIdBody): AnonResponse

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

// ── 请求体 ──
@Serializable
data class DeviceIdBody(val deviceId: String)

@Serializable
data class BookshelfUpsertBody(
    val bookSourceId: String,
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val status: String = "reading",
    val updatedAt: Long,
)

@Serializable
data class ProgressUpsertBody(
    val bookSourceId: String,
    val bookId: String,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val progressPercent: Int = 0,
    val updatedAt: Long,
)

@Serializable
data class BookIdBody(val bookSourceId: String, val bookId: String)

// ── 响应体 ──
@Serializable
data class BackendUser(val id: String, val deviceId: String? = null)

@Serializable
data class AnonResponse(val user: BackendUser)

@Serializable
data class BookshelfDto(
    val bookSourceId: String,
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val status: String = "reading",
    val updatedAt: Long,
)

@Serializable
data class ProgressDto(
    val bookSourceId: String,
    val bookId: String,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val progressPercent: Int = 0,
    val updatedAt: Long,
)

@Serializable
data class SyncResponse(
    val serverTime: Long = 0,
    val bookshelf: List<BookshelfDto> = emptyList(),
    val progress: List<ProgressDto> = emptyList(),
)

/** 写入类接口的通用回执；后端额外返回的 item 字段被 ignoreUnknownKeys 忽略 */
@Serializable
data class UpsertAck(val applied: Boolean = true, val reason: String? = null)

@Serializable
data class OkAck(val ok: Boolean = true)

object BackendClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile
    private var _baseUrl = BuildConfig.BACKEND_BASE_URL

    /** 设备 ID（来自 DataStore），注入到每个请求的 x-device-id 头 */
    private val deviceId = AtomicReference<String?>(null)

    fun setDeviceId(id: String) {
        deviceId.set(id)
    }

    fun setBaseUrl(url: String) {
        if (url != _baseUrl) {
            _baseUrl = url
            api = build(url)
        }
    }

    var api: BackendApi = build(_baseUrl)
        private set

    private fun build(baseUrl: String): BackendApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // 注入设备身份头；后端据此解析匿名用户
                val req = chain.request().newBuilder()
                    .addHeader("x-device-id", deviceId.get() ?: "")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }
}
