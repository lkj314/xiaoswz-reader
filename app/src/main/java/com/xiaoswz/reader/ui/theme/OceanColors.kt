package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 v2.0 视觉令牌（Ocean / 深海陪伴）
 *
 * 来源：VISUAL-SPEC-v2.0.md
 *  - 第三节「色彩体系」精确提取的色值
 *  - 3.3「渐变配方」
 *  - 第五节「圆角与形状规范」
 *
 * 这些令牌不在 Material ColorScheme 内，供 Glass Card / 按钮 / 渐变背景
 * 等自定义组件直接使用。Material 能映射的色（primary/background/surface…）
 * 见 Theme.kt 的 darkColorScheme / lightColorScheme。
 */
object WhaleColors {

    // ── 主色组（来自小鲸服装）──
    val WhaleNavy = Color(0xFF1E4A8A)      // 服装主色
    val WhaleBlue = Color(0xFF5B9FDA)      // 头发渐变梢色 / CTA
    val WhaleLightBlue = Color(0xFF8BB8E8) // 中间过渡色

    // ── 中性色组（深海）──
    val OceanDeep = Color(0xFF0D1B2A)      // 最深背景
    val OceanBase = Color(0xFF142836)      // 默认页面背景
    val OceanSurface = Color(0xFF1E3848)   // 玻璃卡片底色
    val OceanElevated = Color(0xFF2A4656)  // 浮层 / 底栏

    // ── 文字色组 ──
    val TextPrimary = Color(0xFFE8F0F4)
    val TextSecondary = Color(0xFF9AB8C8)
    val TextDisabled = Color(0xFF4A6A7A)
    val TextAccent = Color(0xFF5B9FDA)

    // ── 强调 / 语义色组 ──
    val CtaPrimary = Color(0xFF5B9FDA)
    val CtaHover = Color(0xFF7DB8F5)
    val LoveRose = Color(0xFFE8506A)       // 收藏 / 喜欢（唯一玫瑰色场景）
    val SuccessMint = Color(0xFF4FD1A5)    // 已读完 / 更新已读
    val WarningGold = Color(0xFFF0C256)    // 金币 / 成就（小鲸爱金币）
    val ErrorCoral = Color(0xFFE8636A)     // 错误 / 删除

    // ── 玻璃拟态 ──
    val GlassSurface = Color(0xFF1E3848).copy(alpha = 0.72f) // 半透明深色表面
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)  // 细边框

    // ── 渐变配方（Brush）──
    /** Splash 背景 / 首页顶部头图渐隐：#0D1B2A → #1E4A8A */
    val GradientSplash: Brush
        get() = Brush.verticalGradient(listOf(OceanDeep, WhaleNavy))

    /** 玻璃卡片背景：上深下浅半透明 */
    val GradientCard: Brush
        get() = Brush.verticalGradient(
            listOf(
                Color(0xFF1E3848).copy(alpha = 0.90f),
                Color(0xFF1E3848).copy(alpha = 0.60f),
            )
        )

    /** CTA 主按钮：#5B9FDA → #4A8EDE */
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(WhaleBlue, Color(0xFF4A8EDE)))

    /** 书架顶部渐隐遮罩：透明 → 深海底色 */
    val GradientShelf: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0x000D1B2A), Color(0xFF0D1B2A).copy(alpha = 0.95f))
        )

    /** 阅读器上下渐隐边缘 */
    val GradientReaderEdge: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFF0D1B2A).copy(alpha = 0.80f), Color(0x000D1B2A))
        )
}

/** 圆角系统（VISUAL-SPEC 第五节） */
object WhaleRadius {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 24.dp
    val Full = 999.dp
}
