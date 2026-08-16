package com.xiaoswz.reader.data.plugin

/**
 * 内置官方插件：随 APK 发布，离线即可用。
 *
 * 它们是「创意工坊」的第一个示范，也是 0.11.2 承诺的「划线/书签以插件回归」的落地——
 * 无需联网、无需从广场安装，开箱即激活。用户可在「我的」里停用它们，但无法卸载
 * （它们是 APP 内核阅读能力的一部分，不是第三方插件）。
 *
 * 严格隔离：只描述 APP 自身资源（高亮/书签走 AnnotationRepository，教程是内置页），
 * 不引用主站 novel-site 任何设计/接口。
 */
object BundledPlugins {

    /** 官方高亮：划词高亮，登录后随 AnnotationRepository 跨设备同步 */
    val highlight = PluginManifest(
        id = "official.highlight",
        name = "官方高亮",
        version = 1,
        author = "冲浪阅读官方",
        description = "划词高亮，登录后跨设备同步。",
        icon = "🖍️",
        minAppVersion = 67,
        type = "annotation",
        capabilities = Capabilities(
            annotation = AnnotationCap(
                annotationType = "highlight",
                label = "官方高亮",
                defaultColor = -14336,
                withNote = false,
            ),
        ),
    )

    /** 官方书签：一键书签，可顺手写备注 */
    val bookmark = PluginManifest(
        id = "official.bookmark",
        name = "官方书签",
        version = 1,
        author = "冲浪阅读官方",
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
    )

    /** 教程入口：非功能插件，仅用于广场「教程」置顶展示，点击跳转内置教程页 */
    val tutorial = PluginManifest(
        id = "official.tutorial",
        name = "插件制作教程",
        version = 1,
        author = "冲浪阅读官方",
        description = "不用写代码，会改 JSON 就能做插件。新手第一站。",
        icon = "📘",
        minAppVersion = 67,
        type = "doc",
    )

    /** 全部内置插件（不可卸载，可停用） */
    val all: List<PluginManifest> = listOf(highlight, bookmark, tutorial)
}
