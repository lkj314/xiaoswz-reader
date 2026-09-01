package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 设计系统：冲浪·海洋流动（Ocean Flow）
 * 品牌视觉唯一来源 = 画布设计稿「冲浪阅读美术升级」（Ardot）
 *   海洋渐变签名色：#48CAE4 → #0096C7 → #023E8A
 *
 * 历史：本对象原名 WhaleColors（v2.0 深海陪伴暗色体系），v2.1 曾短暂改为 iOS 浅色系统蓝。
 * 现统一回归冲浪品牌海洋色——强调色 OceanMid / 渐变走海洋梯度，背景为浅青白。
 * 为减少跨文件重构，保留对象名与旧属性名（OceanDeep / OceanSurface / WhaleBlue …），
 * 但色值全部对齐海洋流动设计稿。新代码建议直接用 [GlassTokens] 的语义命名。
 *
 * 这些令牌不在 Material ColorScheme 内，供玻璃卡片 / 按钮 / 渐变背景等自定义组件使用。
 * Material 能映射的色（primary/background/surface…）见 Theme.kt。
 */
object WhaleColors {

    // ── 强调色（海洋品牌）──
    val OceanMid = Color(0xFF0096C7)        // 主强调：CTA / 选中态 / 链接
    val OceanDeep = Color(0xFF023E8A)       // 深海洋：渐变尽头 / 强对比
    val OceanLight = Color(0xFF48CAE4)      // 浅海洋：渐变起头 / 高亮
    val WhaleBlue = Color(0xFF0096C7)       // 兼容旧名 → OceanMid
    val WhaleNavy = Color(0xFF023E8A)       // 兼容旧名 → OceanDeep
    val CtaPrimary = Color(0xFF0096C7)      // 兼容旧名 → OceanMid
    // 兼容旧名（历史 SystemBlue 调用方）
    val SystemBlue = Color(0xFF0096C7)
    val SystemBlueDark = Color(0xFF023E8A)

    // ── 背景（浅青白）──
    val OceanBase = Color(0xFFF2FAFE)       // 浅青白 bg-primary
    val OceanSurface = Color(0xFFFFFFFF)    // 卡片 / 表单项浅底
    val Foam = Color(0xFFCAF0F8)           // 海洋泡沫：分组背景 / 容器

    // ── 文字（墨蓝阶）──
    val TextPrimary = Color(0xFF0A2A3A)    // ink：主文字
    val TextSecondary = Color(0xFF4E7184)  // inkSecondary：辅助文字
    val TextDisabled = Color(0xFF9DB4C2)
    val TextAccent = Color(0xFF0096C7)      // 强调文字：链接 / 数值

    // ── 语义 / 状态色 ──
    val LoveRose = Color(0xFFFF6B6F)       // 涨 / 喜欢（A股习惯：红涨）
    val SuccessMint = Color(0xFF2FB57D)    // 跌 / 成功（A股习惯：绿跌）
    val WarningGold = Color(0xFFFFB347)
    val ErrorCoral = Color(0xFFE5484D)

    // ── 玻璃材质（浅色磨砂）──
    val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.62f)
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.70f)
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.55f)

    // ── 渐变配方（Brush）── 海洋签名渐变 #48CAE4 → #0096C7 → #023E8A
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(OceanLight, OceanMid))

    val GradientOcean: Brush
        get() = Brush.verticalGradient(listOf(OceanLight, OceanMid, OceanDeep))

    val GradientBackground: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFF2FAFE), Color(0xFFEAF6FB), Color(0xFFCAF0F8)),
        )

    val GradientCard: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF).copy(alpha = 0.85f), Color(0xFFFFFFFF).copy(alpha = 0.55f)),
        )

    val GradientSplash: Brush
        get() = GradientOcean

    val GradientShelf: Brush
        get() = GradientBackground

    val GradientReaderEdge: Brush
        get() = GradientBackground
}

/** 圆角系统（沿用命名，值对齐偏大圆角包裹感） */
object WhaleRadius {
    val XS = 4.dp
    val SM = 12.dp
    val MD = 16.dp
    val LG = 20.dp
    val XL = 28.dp
    val Full = 999.dp
}
