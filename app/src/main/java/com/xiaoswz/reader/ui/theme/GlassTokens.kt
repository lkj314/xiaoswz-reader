package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 v2.1 设计令牌（iOS 原生玻璃 / Liquid Glass · 浅色）
 *
 * 参考 Apple Human Interface Guidelines + iOS 26 Liquid Glass 材质语言：
 *  - 系统蓝 #007AFF 作为唯一强调色
 *  - 近白磨砂玻璃材质（半透明 + 高光边 + 柔和投影）
 *  - 浅灰分组背景 #F2F2F7，卡片浮于其上形成层次
 *
 * 这些令牌独立于 Material ColorScheme，供玻璃组件 / 渐变背景直接使用。
 * Material 能映射的色（primary/background/surface…）见 Theme.kt。
 */
object GlassTokens {

    // ── iOS 系统强调色 ──
    val SystemBlue = Color(0xFF007AFF)
    val SystemBlueDark = Color(0xFF0A5CCC)

    // ── 文字层级（iOS label 体系）──
    val Label = Color(0xFF1C1C1E)        // 主文字
    val SecondaryLabel = Color(0xFF8E8E93) // 次要文字
    val TertiaryLabel = Color(0xFFC7C7CC)  // 占位 / 禁用
    val QuaternaryLabel = Color(0xFFAEAEB2)

    // ── 背景层级 ──
    val SystemBackground = Color(0xFFFFFFFF)
    val GroupedBackground = Color(0xFFF2F2F7)

    // ── 分隔线 ──
    val Separator = Color(0x4D3C3C43) // rgba(60,60,67,0.29)

    // ── 语义色（iOS 标准）──
    val Rose = Color(0xFFFF3B30)      // 红：收藏 / 错误
    val Mint = Color(0xFF34C759)      // 绿：已完成 / 更新
    val Gold = Color(0xFFFFCC00)      // 黄：成就

    // ── 玻璃材质（浅色磨砂）──
    /** 卡片填充：近白半透明，叠在背景之上呈磨砂感 */
    val GlassFill = Color(0xFFFFFFFF).copy(alpha = 0.62f)
    /** 强填充：弹出层 / 搜索栏需要更高不透明度 */
    val GlassFillStrong = Color(0xFFFFFFFF).copy(alpha = 0.82f)
    /** 玻璃边缘高光：1dp 亮边模拟光线打在玻璃 rim */
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.70f)
    /** 顶部光泽：从顶边向内渐隐的白色高光 */
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.55f)
    /** 玻璃柔和投影 */
    val GlassShadowColor = Color(0xFF0A1A33).copy(alpha = 0.14f)

    // ── 圆角（iOS 偏大圆角，包裹感强）──
    val RadiusSM = 12.dp
    val RadiusMD = 16.dp
    val RadiusLG = 20.dp
    val RadiusXL = 28.dp
    val RadiusPill = 999.dp

    // ── 渐变配方 ──
    /** CTA 主按钮：systemBlue → systemBlueDark */
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(SystemBlue, SystemBlueDark))

    /** 页面背景：极浅蓝灰渐变，给玻璃材质一点可磨砂的色彩 */
    val GradientBackground: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color(0xFFF4F6FA),
                Color(0xFFEAF0F8),
                Color(0xFFEEF1F9),
            )
        )

    /** 玻璃卡片：上亮下透，强化体积感 */
    val GradientGlass: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.85f),
                Color(0xFFFFFFFF).copy(alpha = 0.55f),
            )
        )
}
