package com.xiaoswz.reader.data.api

import com.xiaoswz.reader.data.plugin.PluginManifest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 创意工坊 · 插件广场（浏览 / 安装计数 / 点赞计数 / 提交）。从 BackendApi.kt 拆分（P2）。
interface PluginApi {

    /** 广场列表：按 type 过滤 + 分页（后端按 置顶→安装量 排序，拉全量 published）。 */
    @GET("api/plugins")
    suspend fun getPlugins(@Query("type") type: String? = null, @Query("page") page: Int = 1): PluginListResponse

    /** 「我的发布」状态查询：按本地记录的提交 id 批量拉取审核状态（含 pending/rejected）。 */
    @GET("api/plugins")
    suspend fun getPluginsStatus(@Query("ids") ids: String): PluginListResponse

    /** 单插件完整清单：安装时拉取 */
    @GET("api/plugins/{pluginId}")
    suspend fun getPluginManifest(@Path("pluginId") pluginId: String): PluginManifestResponse

    /** 提交插件到广场：普通用户落 pending，admin 直发 published。请求体即完整清单。 */
    @POST("api/plugins")
    suspend fun submitPlugin(@Body manifest: PluginManifest): SubmitPluginAck

    /** 安装计数 +1（仅 published 生效） */
    @POST("api/plugins/{pluginId}/install")
    suspend fun installPlugin(@Path("pluginId") pluginId: String): PluginCounterAck

    /** 点赞计数 +1（仅 published 生效） */
    @POST("api/plugins/{pluginId}/like")
    suspend fun likePlugin(@Path("pluginId") pluginId: String): PluginCounterAck
}
