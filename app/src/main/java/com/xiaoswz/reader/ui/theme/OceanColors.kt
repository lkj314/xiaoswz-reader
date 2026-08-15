package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 v2.1 视觉令牌（iOS 原生玻璃 · 浅色）
 *
 * 历史：本对象原名 WhaleColors（v2.0 深海陪伴暗色体系）。v2.1 转向 iOS 原生浅色玻璃风后，
 * 为减少跨文件重构，保留对象名与旧属性名（OceanDeep / OceanSurface / WhaleBlue …），
 * 但色值全部改为浅色 iOS 对应色。新代码建议直接用 [GlassTokens] 的语义命名。
 *
 * 这些令牌不在 Material ColorScheme 内，供玻璃卡片 / 按钮 / 渐变背景等自定义组件使用。
 * Material 能映射的色（primary/background/surface…）见 Theme.kt。
 */
object WhaleColors {

    // ── 强调色（iOS systemBlue）──
    val SystemBlue = Color(0xFF007AFF)
    val SystemBlueDark = Color(0xFF0A5CCC)
    val WhaleBlue = Color(0xFF007AFF)        // 兼容旧名 → systemBlue
    val WhaleNavy = Color(0xFF0A5CCC)        // 兼容旧名 → systemBlueDark
    val CtaPrimary = Color(0xFF007AFF)       // 兼容旧名 → systemBlue

    // ── 背景（浅色）──
    val OceanDeep = Color(0xFFF2F2F7)        // 兼容旧名 → 分组背景
    val OceanBase = Color(0xFFF2F2F7)        // 兼容旧名 → 分组背景
    val OceanSurface = Color(0xFFFFFFFF)     // 兼容旧名 → 卡片/表单项浅底

    // ── 文字（浅色）──
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val TextDisabled = Color(0xFFC7C7CC)
    val TextAccent = Color(0xFF007AFF)

    // ── 语义 / 强调色（iOS 标准）──
    val LoveRose = Color(0xFFFF3B30)
    val SuccessMint = Color(0xFF34C759)
    val WarningGold = Color(0xFFFFCC00)
    val ErrorCoral = Color(0xFFFF3B30)

    // ── 玻璃材质（浅色磨砂）──
    val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.62f)
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.70f)
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.55f)

    // ── 渐变配方（Brush）──
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(SystemBlue, SystemBlueDark))

    val GradientBackground: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFF4F6FA), Color(0xFFEAF0F8), Color(0xFFEEF1F9)),
        )

    val GradientCard: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF).copy(alpha = 0.85f), Color(0xFFFFFFFF).copy(alpha = 0.55f)),
        )

    val GradientSplash: Brush
        get() = GradientButton

    val GradientShelf: Brush
        get() = GradientBackground

    val GradientReaderEdge: Brush
        get() = GradientBackground
}

/** 圆角系统（沿用 v2.0 命名，值对齐 iOS 偏大圆角） */
object WhaleRadius {
    val XS = 4.dp
    val SM = 12.dp
    val MD = 16.dp
    val LG = 20.dp
    val XL = 28.dp
    val Full = 999.dp
}
