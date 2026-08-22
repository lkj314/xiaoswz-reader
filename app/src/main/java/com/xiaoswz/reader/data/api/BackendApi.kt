package com.xiaoswz.reader.data.api

import com.xiaoswz.reader.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// BackendApi 聚合接口（P2 文件拆分）。
//
// 原先 1500+ 行的单一 interface 已按业务域拆成 15 个独立接口
// （AuthApi / SyncApi / BookApi / EngagementApi / CharacterApi / AdminApi /
//  AdsApi / CommunityApi / BooklistApi / UserApi / HomeApi / CatalogApi /
//  AnnotationApi / PluginApi / ReadingApi），各自成文件，便于并行维护。
//
// 本文件仅做两件事：
//   1. `interface BackendApi` 通过 Kotlin 接口继承把上述域接口重新聚合，
//      因此所有调用点 `BackendClient.api.xxx()` 完全不变；
//   2. 保留 `BackendClient`（Retrofit 实例 + 鉴权头注入）与 `RetryInterceptor`。
//
// 请求/响应 DTO 全部迁移到同包的 BackendModels.kt。

interface BackendApi :
    AuthApi,
    SyncApi,
    BookApi,
    EngagementApi,
    CharacterApi,
    AdminApi,
    AdsApi,
    CommunityApi,
    BooklistApi,
    UserApi,
    HomeApi,
    CatalogApi,
    AnnotationApi,
    PluginApi,
    AuthorLogApi,
    ReadingApi

object BackendClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile
    private var _baseUrl = BuildConfig.BACKEND_BASE_URL

    /** 设备 ID（来自 DataStore），注入到每个请求的 x-device-id 头 */
    private val deviceId = AtomicReference<String?>(null)

    /** 登录后的 JWT；非空时注入 Authorization: Bearer 头（优先级高于 x-device-id） */
    private val authToken = AtomicReference<String?>(null)

    fun setDeviceId(id: String) {
        deviceId.set(id)
    }

    /** 写入 / 清除登录令牌。传入 null 即登出态（后续请求回落到匿名设备身份） */
    fun setAuthToken(token: String?) {
        authToken.set(token)
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
                // 注入设备身份头（匿名兜底）；已登录时额外带 Bearer，后端据此解析用户账号
                val reqBuilder = chain.request().newBuilder()
                    .addHeader("x-device-id", deviceId.get() ?: "")
                authToken.get()?.let { reqBuilder.addHeader("Authorization", "Bearer $it") }
                chain.proceed(reqBuilder.build())
            }
            // 轻量重试（0.9.0）：serverless 冷启动偶发连接抖动，单次 IOException 重试一次，
            // 显著改善「首次打开偶发空白/转圈」；不影响鉴权/业务状态码（只重试传输层异常）。
            .addInterceptor(RetryInterceptor())
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

/** 传输层重试：仅对 IOException（连接/超时抖动，常见于 serverless 冷启动）重试一次 */
private class RetryInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var attempt = 0
        var last: java.io.IOException? = null
        while (attempt < 2) {
            attempt++
            try {
                return chain.proceed(chain.request())
            } catch (e: java.io.IOException) {
                last = e
                if (attempt >= 2) throw e
                try {
                    Thread.sleep(800)
                } catch (_: InterruptedException) {
                    // 被打断直接退出等待
                }
            }
        }
        throw last ?: java.io.IOException("retry exhausted")
    }
}
