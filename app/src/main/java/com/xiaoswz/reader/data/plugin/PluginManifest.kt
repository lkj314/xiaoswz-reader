package com.xiaoswz.reader.data.plugin

import kotlinx.serialization.Serializable

/**
 * 创意工坊插件清单（v1 声明式，无动态代码）。
 *
 * 设计铁律：插件只能是「数据」，由 APP 预置的能力槽（见 [Capabilities]）解释执行。
 * 这彻底消灭「插件崩溃主程序」「插件劫持输入」整类风险，也让核心阅读体验（滚动/翻页/纯文本）
 * 永不因插件受损。详见《创意工坊设计文档.md》。
 *
 * 颜色统一用 ARGB Int（如 -14336）。
 */
@Serializable
data class PluginManifest(
    /** 反向域名，全局唯一主键，用作启用/卸载/数据命名空间 */
    val id: String,
    val name: String,
    val version: Int = 1,
    val author: String = "",
    val description: String = "",
    /** emoji 或远程图标 URL（后续支持） */
    val icon: String = "\uD83E\uDDE6",
    /** versionCode 下限，低于此值的 APP 对该插件能力静默降级 */
    val minAppVersion: Int = 67,
    /** 插件家族，决定主能力槽：annotation | theme | toolbar | decorator */
    val type: String,
    val capabilities: Capabilities = Capabilities(),
)

/** 按 [PluginManifest.type] 填充对应能力对象；未知能力忽略 */
@Serializable
data class Capabilities(
    val annotation: AnnotationCap? = null,
    val theme: ThemeCap? = null,
    val toolbar: ToolbarCap? = null,
    val decorator: DecoratorCap? = null,
)

/**
 * 选区动作槽：选中文字后，系统选区菜单里追加本插件的 [label] 项。
 * 点击 → 写入 AnnotationEntity（type=[annotationType]，offset 相对预处理正文，与渲染同源）。
 */
@Serializable
data class AnnotationCap(
    /** 自定义类型，写入 AnnotationEntity.type（开放字符串，无需改表） */
    val annotationType: String,
    /** 选区菜单显示名，如「高亮」「书签」 */
    val label: String,
    /** ARGB 高亮色；书签可为 null */
    val defaultColor: Int? = null,
    /** 是否允许顺手写备注 */
    val withNote: Boolean = false,
)

/** 主题槽：贡献一条阅读配色，出现在阅读设置「主题」选择器 */
@Serializable
data class ThemeCap(
    val name: String,
    val background: Int,
    val text: Int,
)

/** 工具栏动作槽：在顶/底栏注册一个 IconButton，仅点击触发，不挂手势 */
@Serializable
data class ToolbarCap(
    /** copy_page | share | open_sheet | custom */
    val action: String,
    val label: String,
    /** top | bottom */
    val position: String = "bottom",
)

/** 标注渲染槽：对某个类型的标注加背景/下划线（复用 0.11.6 AnnotatedString 锚点范式） */
@Serializable
data class DecoratorCap(
    /** 渲染哪种 annotation 类型 */
    val targetType: String,
    /** background | underline */
    val style: String = "background",
    val color: Int? = null,
)

/** 单条已装插件的状态（清单 + 是否启用） */
@Serializable
data class PluginInstall(
    val manifest: PluginManifest,
    val enabled: Boolean = true,
)
