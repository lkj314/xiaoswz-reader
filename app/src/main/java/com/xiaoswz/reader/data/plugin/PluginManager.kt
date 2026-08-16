package com.xiaoswz.reader.data.plugin

/**
 * 插件注册表：纯函数式能力槽筛选。数据来源（内置 + DataStore 已装）由 [PluginRepository] 提供，
 * 本类只负责「给定一份 manifest 列表，按能力槽归类、做版本门控」。无 Context / 无副作用，
 * 便于组合层直接调用，也便于单测。
 *
 * 新增一种扩展点：在 [Capabilities] 加一个字段 + 这里加一个 `xxxPlugins` 方法即可。
 */
object PluginManager {

    /** 当前 APP 的 versionCode，用于 [PluginManifest.minAppVersion] 门控 */
    var currentAppVersion: Int = 67

    /** 全部已启用且满足版本门控的插件 */
    fun filterActive(manifests: List<PluginManifest>): List<PluginManifest> =
        manifests.filter { it.minAppVersion <= currentAppVersion }

    /** 选区动作类插件（annotation 槽）：供 SelectionContainer 选区菜单动态追加 */
    fun annotationPlugins(manifests: List<PluginManifest>): List<PluginManifest> =
        manifests.filter { it.type == "annotation" && it.capabilities.annotation != null }

    /** 主题类插件：供主题选择器 */
    fun themePlugins(manifests: List<PluginManifest>): List<PluginManifest> =
        manifests.filter { it.type == "theme" && it.capabilities.theme != null }

    /** 工具栏动作类插件：供顶/底栏注入按钮 */
    fun toolbarPlugins(manifests: List<PluginManifest>): List<PluginManifest> =
        manifests.filter { it.type == "toolbar" && it.capabilities.toolbar != null }

    /** 标注渲染类插件：供 decorator 槽（AnnotatedString 锚点渲染） */
    fun decoratorPlugins(manifests: List<PluginManifest>): List<PluginManifest> =
        manifests.filter { it.type == "decorator" && it.capabilities.decorator != null }

    /** 取装饰某 annotation 类型所需的 decorator（首条命中即可） */
    fun decoratorFor(manifests: List<PluginManifest>, annotationType: String): DecoratorCap? =
        decoratorPlugins(manifests).firstNotNullOfOrNull { it.capabilities.decorator }
            ?.takeIf { it.targetType == annotationType }
}
