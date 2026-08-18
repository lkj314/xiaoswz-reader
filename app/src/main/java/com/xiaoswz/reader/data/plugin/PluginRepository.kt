package com.xiaoswz.reader.data.plugin

import android.content.Context
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.PluginCapabilitiesDto
import com.xiaoswz.reader.data.api.PluginManifestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * 插件中央仓储：合并「用户从广场安装 / 外部导入的插件（DataStore）」，统一做版本门控与启用过滤，
 * 并向组合层 / 能力槽提供查询。
 *
 * 0.16.3 重构：官方高亮/书签已**解耦为阅读器内置标注能力**（见 [builtinAnnotationManifests]），
 * 不再是「插件」实体，不进插件列表 / 广场 / 我的插件。创意工坊只承载用户创作 / 导入的扩展，
 * 对齐 Obsidian（核心插件 vs 社区插件分离）/ VS Code（编辑器核心 vs Marketplace 扩展）的成熟范式。
 *
 * 隔离铁律：只消费 APP 自身资源（AnnotationRepository / DataStore）与冲浪阅读独立后端；
 * 网络仅经自有后端或用户显式提供的导入 URL（https）。
 */
object PluginRepository {

    /**
     * 阅读器内置标注能力（高亮 / 书签）—— 它们是 APP 内核阅读能力的一部分，非第三方插件。
     * 始终出现在划词选区菜单（受阅读器 annoActive 总开关控制），不可停用、不进插件体系。
     * 这是把「官方高亮/书签」从 BundledPlugins 里剥离、回归内置的正确落点。
     */
    private val builtinAnnotationManifests = listOf(
        PluginManifest(
            id = "builtin.highlight",
            name = "高亮",
            version = 1,
            author = "冲浪阅读",
            description = "划词高亮，登录后跨设备同步。",
            icon = "🖍️",
            minAppVersion = 67,
            type = "annotation",
            capabilities = Capabilities(
                annotation = AnnotationCap(
                    annotationType = "highlight",
                    label = "高亮",
                    defaultColor = -14336,
                    withNote = false,
                ),
            ),
        ),
        PluginManifest(
            id = "builtin.bookmark",
            name = "书签",
            version = 1,
            author = "冲浪阅读",
            description = "一键书签，可写备注。",
            icon = "🔖",
            minAppVersion = 67,
            type = "annotation",
            capabilities = Capabilities(
                annotation = AnnotationCap(
                    annotationType = "bookmark",
                    label = "加书签",
                    defaultColor = null,
                    withNote = true,
                ),
            ),
        ),
    )

    /** 全部「生效中」的用户插件 manifest（已安装 + 启用 + 版本门控） */
    fun activeManifestsFlow(ctx: Context): Flow<List<PluginManifest>> =
        PluginStateStore.installedFlow(ctx).map { installs ->
            installs.filter { it.enabled }.map { it.manifest }
                .let { PluginManager.filterActive(it) }
        }

    // ── 能力槽查询（委托 PluginManager 纯函数）──
    /**
     * 选区动作类插件：内置高亮/书签始终在前，用户安装的 annotation 插件追加在后。
     * 内置项不受「停用」影响（它们是阅读器自身能力）；用户插件仍受各自启用状态控制。
     */
    fun annotationPlugins(ctx: Context): Flow<List<PluginManifest>> =
        activeManifestsFlow(ctx).map { installed ->
            (builtinAnnotationManifests + PluginManager.annotationPlugins(installed))
                .distinctBy { it.id }
                .let { PluginManager.filterActive(it) }
        }

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

    /** 安装本地 / 外部导入的清单（只本机生效，不可发布） */
    suspend fun installLocal(ctx: Context, manifest: PluginManifest) {
        PluginStateStore.install(ctx, manifest, enabled = true)
    }

    /**
     * 从 manifest JSON 文本导入（校验基本字段后安装到本机）。
     * 对应 Obsidian 手动放入插件目录 / VS Code 从 VSIX 安装——去中心化的「导入」通道。
     */
    suspend fun importFromText(ctx: Context, json: String): Result<PluginManifest> =
        runCatching {
            val manifest = PluginManifest.fromJson(json)
                ?: throw IllegalArgumentException("清单解析失败或字段缺失（需含 id/name/type）")
            PluginStateStore.install(ctx, manifest, enabled = true)
            manifest
        }

    /** 从 https URL 导入清单（用户外部托管，如 GitHub raw）。限 https，避免明文流量。 */
    suspend fun importFromUrl(ctx: Context, url: String): Result<PluginManifest> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = URL(url).readText()
                importFromText(ctx, text).getOrThrow()
            }
        }

    suspend fun uninstall(ctx: Context, pluginId: String) {
        PluginStateStore.uninstall(ctx, pluginId)
    }

    suspend fun setEnabled(ctx: Context, pluginId: String, enabled: Boolean) {
        PluginStateStore.setEnabled(ctx, pluginId, enabled)
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
