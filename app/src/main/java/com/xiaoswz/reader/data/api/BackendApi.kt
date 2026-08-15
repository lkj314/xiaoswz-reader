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
import retrofit2.http.Path
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

    /** 邮箱注册 / 升级本机匿名账号（带 deviceId 时自动绑定书架与进度） */
    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRegisterBody): AuthResponse

    /** 邮箱 + 密码登录（用户 / 管理员共用入口） */
    @POST("api/auth/login")
    suspend fun login(@Body body: AuthLoginBody): AuthResponse

    /** 当前登录身份（Bearer 鉴权；用于刷新角色 / 禁言状态） */
    @GET("api/auth/me")
    suspend fun me(): AuthMeResponse

    /** 管理员 SSO 兑换：拿登录 JWT 换一个 60 秒单次票据（避免长期 JWT 进浏览器 URL） */
    @POST("api/admin/sso/exchange")
    suspend fun exchangeAdminSso(): SsoExchangeResponse

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

    // ── P1 书库发现 / 聚合 ──
    /** 上报浏览/曝光并回写元数据（打开详情页时调用） */
    @POST("api/books/{src}/{id}")
    suspend fun reportBookView(
        @Path("src") src: String,
        @Path("id") id: String,
        @Body body: BookViewBody,
    ): OkAck

    /** 单书聚合指标（月票/评分/评论数/浏览量） */
    @GET("api/books/{src}/{id}")
    suspend fun getBookStats(
        @Path("src") src: String,
        @Path("id") id: String,
    ): BookStatsResponse

    /** 榜单（popularity / monthly / rating / new） */
    @GET("api/leaderboards/{board}")
    suspend fun getLeaderboard(@Path("board") board: String): LeaderboardResponse

    // ── P2 月票 / 评分 / 评论 ──
    /** 投月票/推荐票 */
    @POST("api/votes")
    suspend fun castVote(@Body body: VoteBody): VoteResult

    /** 我的剩余票数 */
    @GET("api/votes")
    suspend fun getVoteBalance(): VoteBalance

    /** 提交/修改评分 */
    @POST("api/ratings")
    suspend fun submitRating(@Body body: RatingBody): OkAck

    /** 平均分 + 我的评分 */
    @GET("api/ratings")
    suspend fun getRating(
        @Query("bookSourceId") src: String,
        @Query("bookId") id: String,
    ): RatingResponse

    /** 评论列表 */
    @GET("api/books/{src}/{id}/comments")
    suspend fun getComments(
        @Path("src") src: String,
        @Path("id") id: String,
        @Query("page") page: Int = 1,
    ): CommentListResponse

    /** 发评论 / 楼中楼 */
    @POST("api/books/{src}/{id}/comments")
    suspend fun postComment(
        @Path("src") src: String,
        @Path("id") id: String,
        @Body body: CommentBody,
    ): OkAck

    /** 评论点赞 */
    @POST("api/comments/{id}/like")
    suspend fun likeComment(@Path("id") id: String): OkAck

    /** 评论举报（基础风控，自增 reportCount，审核后台预留） */
    @POST("api/comments/{id}/report")
    suspend fun reportComment(@Path("id") id: String): OkAck

    // ── P3 广告 / 归因 ──
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
data class AnonResponse(val token: String = "", val user: BackendUser)

// ── 登录账号（user / admin）──
@Serializable
data class AuthRegisterBody(val email: String, val password: String, val deviceId: String? = null)

@Serializable
data class AuthLoginBody(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: AuthUser)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    val role: String = "guest",
    val displayName: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class AuthMeResponse(
    val id: String,
    val email: String? = null,
    val role: String = "guest",
    val displayName: String? = null,
    val deviceId: String? = null,
    val mutedUntil: Long? = null,
)

@Serializable
data class SsoExchangeResponse(val ok: Boolean = false, val sso: String = "", val ttlSec: Int = 0)

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

// ════════════════════════════════════════════════════════════════
//  P1–P3 数据表（冲浪阅读独立后端）对应 DTO
// ════════════════════════════════════════════════════════════════

// 书源常量：当前唯一书源 = 主站。绝不接外部源（铁律）。
const val BOOK_SOURCE_MAIN = "main"

// ── P1 书库聚合 ──
@Serializable
data class BookViewBody(
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class BookStatsResponse(
    val found: Boolean = false,
    val bookSourceId: String? = null,
    val bookId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val voteCount: Long = 0,
    val voteMonth: Int = 0,
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Long = 0,
    val favoriteCount: Int = 0,
)

@Serializable
data class LeaderboardResponse(
    val board: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
)

@Serializable
data class LeaderboardEntry(
    val bookSourceId: String,
    val bookId: String,
    val title: String? = null,
    /** 绝对 http(s) 封面，可缓存；主站 data: 在响应里会走 coverDataUri 而不是这个字段 */
    val coverUrl: String? = null,
    /** 主站内嵌 data:image/...;base64 封面的临时回退（不入 book_stats，仅响应级） */
    val coverDataUri: String? = null,
    val metric: Double = 0.0,
)

// ── P2 月票 / 评分 / 评论 ──
@Serializable
data class VoteBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val voteType: String = "monthly", // monthly / recommend
)

@Serializable
data class VoteResult(
    val ok: Boolean = true,
    val reason: String? = null,
    val remaining: Int = 0,
)

@Serializable
data class VoteBalance(
    val dailyLimit: Int = 5,
    val used: Int = 0,
    val remaining: Int = 5,
)

@Serializable
data class RatingBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val score: Int,
)

@Serializable
data class RatingResponse(
    val avg: Double = 0.0,
    val count: Int = 0,
    val myScore: Int = 0,
)

@Serializable
data class CommentListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val comments: List<CommentItem> = emptyList(),
)

@Serializable
data class CommentItem(
    val id: String,
    val parentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = 0,
)

@Serializable
data class CommentBody(
    val content: String,
    val parentId: String? = null,
)

// ── P3 广告 / 归因 ──
@Serializable
data class AdResponse(
    val slot: String? = null,
    val creatives: List<AdCreativeDto> = emptyList(),
)

@Serializable
data class AdCreativeDto(
    val id: String,
    val kind: String? = null, // cross_promo / third_party
    val title: String? = null,
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val bookSourceId: String? = null,
    val bookId: String? = null,
)

@Serializable
data class AdImpressionBody(
    val creativeId: String? = null,
    val slot: String,
)

@Serializable
data class AdClickBody(
    val impressionId: String? = null,
)

@Serializable
data class AttributionBody(
    val installChannel: String? = null,
    val referrer: String? = null,
    val campaign: String? = null,
)
