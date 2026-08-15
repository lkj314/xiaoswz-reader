package com.xiaoswz.reader.data.auth

import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.api.AuthLoginBody
import com.xiaoswz.reader.data.api.AuthRegisterBody
import com.xiaoswz.reader.data.api.AuthResponse
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * 登录账号仓库：封装邮箱注册 / 登录 / 会话刷新 / 登出。
 *
 * 设计要点：
 * 1. 注册时携带本机 deviceId，后端据此把该匿名设备账号「升级」为 user（书架/进度/评分自动归属，
 *    不会丢数据）；不传 deviceId 则新建独立账号。
 * 2. 成功后把 JWT 写入 DataStore 并注入 [BackendClient] 的 Bearer 拦截器，
 *    后续所有评论 / 云同步等需鉴权请求自动带上身份；登出则回落到匿名设备账号。
 * 3. 启动时用 [applyStoredToken] 恢复会话，[refreshSession] 拉取最新角色 / 禁言状态。
 */
object AuthRepository {

    private val appSettings by lazy { AppSettingsRepository(AppContext.app) }

    /** 启动时把持久化的令牌注入 BackendClient（Bearer 拦截器），恢复登录态 */
    suspend fun applyStoredToken() {
        BackendClient.setAuthToken(appSettings.getAuthToken())
    }

    /** 邮箱注册（携带 deviceId 自动升级本机匿名账号） */
    suspend fun register(email: String, password: String, deviceId: String?): AuthResult {
        return runAuth {
            val resp = BackendClient.api.register(AuthRegisterBody(email, password, deviceId))
            persistAndInject(resp)
            AuthResult.Ok
        }
    }

    /** 邮箱 + 密码登录（用户 / 管理员共用入口） */
    suspend fun login(email: String, password: String): AuthResult {
        return runAuth {
            val resp = BackendClient.api.login(AuthLoginBody(email, password))
            persistAndInject(resp)
            AuthResult.Ok
        }
    }

    /**
     * 会话刷新：用本地 token 拉取最新身份（角色 / 禁言状态）。
     * 令牌失效（401）则清理本地登录态；网络异常则沿用本地会话，不强制登出。
     */
    suspend fun refreshSession(): AuthResult {
        val token = appSettings.getAuthToken() ?: return AuthResult.Error("未登录")
        return try {
            val me = BackendClient.api.me()
            appSettings.saveAccount(
                token = token,
                id = me.id,
                email = me.email ?: "",
                role = me.role,
                mutedUntil = me.mutedUntil ?: 0L,
            )
            AuthResult.Ok
        } catch (e: HttpException) {
            if (e.code() == 401) {
                logout()
                AuthResult.Error("登录已失效，请重新登录")
            } else {
                AuthResult.Error(mapHttpError(e.code(), e))
            }
        } catch (e: IOException) {
            AuthResult.Error("网络不可达，沿用本地会话")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "刷新失败")
        }
    }

    /** 登出：清除本地账号 + 撤销 Bearer 注入（回落匿名设备账号） */
    suspend fun logout() {
        appSettings.clearAccount()
        BackendClient.setAuthToken(null)
    }

    private suspend fun persistAndInject(resp: AuthResponse) {
        appSettings.saveAccount(
            token = resp.token,
            id = resp.user.id,
            email = resp.user.email ?: "",
            role = resp.user.role,
        )
        BackendClient.setAuthToken(resp.token)
    }

    private suspend fun runAuth(block: suspend () -> AuthResult): AuthResult {
        return withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: HttpException) {
                AuthResult.Error(mapHttpError(e.code(), e))
            } catch (e: IOException) {
                AuthResult.Error("网络不可达，请检查后端连接")
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "操作失败")
            }
        }
    }

    private fun mapHttpError(code: Int, e: HttpException): String {
        val errCode = try {
            e.response()?.errorBody()?.string()?.let { body ->
                Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            }
        } catch (_: Exception) {
            null
        }
        return when (errCode) {
            "EMAIL_TAKEN" -> "该邮箱已注册，请直接登录"
            "DEVICE_REGISTERED" -> "本设备已绑定其他账号"
            "BAD_INPUT" -> "邮箱格式或密码长度（至少 8 位）不正确"
            "invalid_credentials" -> "邮箱或密码错误"
            else -> when (code) {
                401 -> "邮箱或密码错误"
                409 -> "账号冲突，请尝试登录"
                400 -> "请求参数不正确"
                else -> "操作失败（错误 $code）"
            }
        }
    }
}

sealed class AuthResult {
    object Ok : AuthResult()
    data class Error(val message: String) : AuthResult()
}
