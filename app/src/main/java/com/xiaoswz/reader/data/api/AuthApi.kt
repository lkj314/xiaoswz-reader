package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 账号体系：匿名设备登录 / 邮箱注册登录 / 身份刷新 / 管理员 SSO 兑换。
// 从 BackendApi.kt 拆分（P2），聚合进 BackendApi。方法签名逐字保留。
interface AuthApi {

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
}
