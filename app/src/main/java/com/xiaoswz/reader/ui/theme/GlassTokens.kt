package com.xiaoswz.reader.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 冲浪阅读 设计令牌（Meta 00003 · 浅色）
 * 白底灰边 · 钴蓝强调 · 墨阶文字 · 发丝边分隔线 · 扁平白卡（去除 iOS 玻璃光斑）。
 * 这些令牌独立于 Material ColorScheme，供玻璃组件 / 渐变背景直接使用。
 */
object GlassTokens {

    // ── 钴蓝强调 ──
    val SystemBlue = Color(0xFF0064E0)
    val SystemBlueDark = Color(0xFF0457CB)
    val SystemRed = Color(0xFFE41E3F)

    // ── 文字层级（Meta label 体系）──
    val Label = Color(0xFF0A1317)        // 主文字 Ink Deep
    val SecondaryLabel = Color(0xFF5D6C7B) // 次要文字 Steel
    val TertiaryLabel = Color(0xFF8595A4)  // 占位 / 禁用
    val QuaternaryLabel = Color(0xFFAEB4BA)

    // ── 背景层级 ──
    val SystemBackground = Color(0xFFFFFFFF)
    val GroupedBackground = Color(0xFFF1F4F7)

    // ── 分隔线（发丝边）──
    val Separator = Color(0xFFCED0D4)

    // ── 语义色（A股：红涨绿跌）──
    val Rose = Color(0xFFE41E3F)      // 红：涨 / 危险
    val Mint = Color(0xFF31A24C)      // 绿：跌 / 成功
    val Gold = Color(0xFFF2A918)      // 黄：注意

    // ── 玻璃材质（Meta 扁平白卡 + 发丝边）──
    val GlassFill = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val GlassFillStrong = Color(0xFFFFFFFF)
    val GlassBorder = Color(0xFFDEE1E6)
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.60f)
    val GlassShadowColor = Color(0xFF0A1317).copy(alpha = 0.08f)

    // ── 圆角（Meta 紧凑）──
    val RadiusSM = 4.dp
    val RadiusMD = 8.dp
    val RadiusLG = 12.dp
    val RadiusXL = 16.dp
    val RadiusPill = 999.dp

    // ── 渐变配方 ──
    val GradientButton: Brush
        get() = Brush.verticalGradient(listOf(SystemBlue, SystemBlueDark))

    val GradientBackground: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFF6F7F9)),
        )

    val GradientGlass: Brush
        get() = Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFFBFBFC)),
        )
}
