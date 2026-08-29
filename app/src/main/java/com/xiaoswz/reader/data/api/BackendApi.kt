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
// 原先 1500+ 行的单一 interface 已按业务域拆成 17 个独立接口
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
    CoinApi,
    BookCircleApi,
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
            val old = clientRef.getAndSet(null)
            api = build(url)
            // 修复（M20）：旧 client 不再被使用，回收其线程池/连接池，
            // 否则每改一次后端地址就泄漏一组线程与连接。
            old?.dispatcher?.executorService?.shutdown()
        }
    }

    /** 最近一次构建的 OkHttpClient，用于换后端地址时回收旧实例（M20） */
    private val clientRef = AtomicReference<OkHttpClient?>(null)

    // 修复（M20）：api 会被 setBaseUrl 从任意线程替换，必须 @Volatile 保证跨线程可见
    @Volatile
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

        clientRef.set(client)

        return Retrofit.Builder()
            .baseUrl(baseUrl + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }
}

/**
 * 传输层重试：仅对 IOException（连接/超时抖动，常见于 serverless 冷启动）重试一次。
 *
 * 修复（H8）：只对幂等方法重试。POST（认购/转账/发帖等）在「服务端已处理、响应丢失」时
 * 重试会造成重复扣款/重复发帖，故非幂等方法一律直接放行、失败即抛出，绝不重试。
 * 另外重试等待前先检查 call 是否已取消（用户退出页面/超时），避免在 sleep 里空等。
 */
private class RetryInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        // 非幂等方法（POST/PUT/PATCH/DELETE...）：不重试
        if (request.method !in IDEMPOTENT_METHODS) return chain.proceed(request)

        var attempt = 0
        var last: java.io.IOException? = null
        while (attempt < 2) {
            attempt++
            try {
                return chain.proceed(request)
            } catch (e: java.io.IOException) {
                last = e
                if (attempt >= 2) throw e
                // 已取消（用户退出/超时）：不再等待，直接抛出
                if (chain.call().isCanceled()) throw e
                try {
                    Thread.sleep(800)
                } catch (_: InterruptedException) {
                    // 被打断：恢复中断标记后立即重试
                    Thread.currentThread().interrupt()
                }
            }
        }
        throw last ?: java.io.IOException("retry exhausted")
    }

    companion object {
        /** 可安全重试的幂等方法；其余方法（尤其 POST 写操作）一律不重试 */
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
