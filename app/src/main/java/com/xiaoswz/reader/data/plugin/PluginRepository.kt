package com.xiaoswz.reader.data.plugin

import android.content.Context
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.PluginCapabilitiesDto
import com.xiaoswz.reader.data.api.PluginManifestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 插件中央仓储：合并「内置官方插件（bundled，随 APK，离线可用）」+「用户从广场安装 / DIY 的插件
 * （DataStore）」，统一做去重、版本门控与启用过滤，并向组合层 / 能力槽提供查询。
 *
 * 这是 0.12.0 创意工坊 v1 的数据中枢：四 Tab UI 与阅读器各能力槽都从这里取数。
 *
 * 隔离铁律：只消费 APP 自身资源（AnnotationRepository / DataStore / 内置清单），不碰主站任何设计/接口；
 * 网络仅经冲浪阅读自有后端（Plugin 表在独立数据库，与主站零共享）。
 */
object PluginRepository {

    /** 全部「生效中」的插件 manifest（bundled ∪ installed，已启用 + 版本门控 + 内置停用覆盖） */
    fun activeManifestsFlow(ctx: Context): Flow<List<PluginManifest>> =
        combine(
            PluginStateStore.installedFlow(ctx),
            PluginStateStore.disabledBundledFlow(ctx),
        ) { installs, disabledBundled ->
            val installed = installs.filter { it.enabled }.map { it.manifest }
            val bundled = BundledPlugins.all.filter { it.id !in disabledBundled }
            (bundled + installed)
                .distinctBy { it.id }
                .let { PluginManager.filterActive(it) }
        }

    // ── 能力槽查询（委托 PluginManager 纯函数）──
    fun annotationPlugins(ctx: Context): Flow<List<PluginManifest>> =
        activeManifestsFlow(ctx).map { PluginManager.annotationPlugins(it) }

    fun themePlugins(ctx: Context): Flow<List<PluginManifest>> =
        activeManifestsFlow(ctx).map { PluginManager.themePlugins(it) }

    fun toolbarPlugins(ctx: Context): Flow<List<PluginManifest>> =
        activeManifestsFlow(ctx).map { PluginManager.toolbarPlugins(it) }

    fun decoratorPlugins(ctx: Context): Flow<List<PluginManifest>> =
        activeManifestsFlow(ctx).map { PluginManager.decoratorPlugins(it) }

    /** 取装饰某 annotation 类型所需的 decorator（首条命中即可） */
    suspend fun decoratorFor(ctx: Context, annotationType: String): DecoratorCap? =
        PluginManager.decoratorFor(decoratorPlugins(ctx).first(), annotationType)

    // ── 安装 / 卸载 / 启用 ──
    /** 从广场拉取清单并安装（写入 DataStore，默认启用） */
    suspend fun installFromNetwork(ctx: Context, pluginId: String): Result<PluginManifest> =
        runCatching {
            val resp = BackendClient.api.getPluginManifest(pluginId)
            val manifest = resp.manifest.toAppModel()
            PluginStateStore.install(ctx, manifest, enabled = true)
            manifest
        }

    /** 安装本地 DIY 清单（只本机生效，不可发布） */
    suspend fun installLocal(ctx: Context, manifest: PluginManifest) {
        PluginStateStore.install(ctx, manifest, enabled = true)
    }

    suspend fun uninstall(ctx: Context, pluginId: String) {
        PluginStateStore.uninstall(ctx, pluginId)
    }

    suspend fun setEnabled(ctx: Context, pluginId: String, enabled: Boolean) {
        // 内置插件走「停用覆盖」集合；DIY / 广场插件走已装列表。
        if (BundledPlugins.all.any { it.id == pluginId }) {
            PluginStateStore.setBundledDisabled(ctx, pluginId, !enabled)
        } else {
            PluginStateStore.setEnabled(ctx, pluginId, enabled)
        }
    }
}

/** 后端清单 DTO → APP 端 PluginManifest */
fun PluginManifestDto.toAppModel(): PluginManifest = PluginManifest(
    id = id,
    name = name,
    version = version,
    author = author,
    description = description,
    icon = icon,
    minAppVersion = minAppVersion,
    type = type,
    capabilities = capabilities.toAppModel(),
)

private fun PluginCapabilitiesDto.toAppModel() = Capabilities(
    annotation = annotation?.let {
        AnnotationCap(it.annotationType, it.label, it.defaultColor, it.withNote)
    },
    theme = theme?.let { ThemeCap(it.name, it.background, it.text) },
    toolbar = toolbar?.let { ToolbarCap(it.action, it.label, it.position) },
    decorator = decorator?.let { DecoratorCap(it.targetType, it.style, it.color) },
)
