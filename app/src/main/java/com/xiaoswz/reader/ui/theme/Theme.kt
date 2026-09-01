package com.xiaoswz.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 冲浪阅读 设计系统：冲浪·海洋流动（Ocean Flow）
// 品牌视觉唯一来源 = 画布设计稿「冲浪阅读美术升级」（Ardot）
//   海洋渐变签名色：#48CAE4 → #0096C7 → #023E8A
//   DARK  = 深海洋蓝黑（品牌默认）
//   LIGHT = 浅青白 + 海洋蓝强调（保持浅色主题切换可用）
// 设计令牌（色板 / 圆角 / 字阶）与设计稿严格对齐。
// ─────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = Color(0xFF48CAE4),          // oceanLight：CTA / 选中态 / 强调
    onPrimary = Color(0xFF04141F),
    primaryContainer = Color(0xFF023E8A), // oceanDeep
    onPrimaryContainer = Color(0xFFCAF0F8),
    secondary = Color(0xFF90E0EF),
    onSecondary = Color(0xFF04141F),
    secondaryContainer = Color(0xFF0F2E44),
    onSecondaryContainer = Color(0xFFCAF0F8),
    tertiary = Color(0xFF4FD1A5),         // success-mint（语义强调备用）
    onTertiary = Color(0xFF06281D),
    background = Color(0xFF04141F),       // 深海洋蓝黑 ocean-base
    onBackground = Color(0xFFEAF6FB),
    surface = Color(0xFF0A2436),          // ocean-surface
    onSurface = Color(0xFFEAF6FB),
    surfaceVariant = Color(0xFF0F2E44),    // ocean-elevated（底栏 / 浮层）
    onSurfaceVariant = Color(0xFF9DC3D4),
    outline = Color(0xFF1C3D52),
    outlineVariant = Color(0xFF0F2E44),
    error = Color(0xFFE8636A),
    onError = Color(0xFFFFFFFF),
)

// 浅青白 + 海洋蓝强调（iOS 玻璃风升级为品牌海洋风）
private val LightColors = lightColorScheme(
    primary = Color(0xFF0096C7),          // oceanMid：CTA / 选中态 / 强调
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAF0F8), // foam
    onPrimaryContainer = Color(0xFF023E8A),
    secondary = Color(0xFF48CAE4),
    onSecondary = Color(0xFF04141F),
    secondaryContainer = Color(0xFFEAF6FB),
    onSecondaryContainer = Color(0xFF023E8A),
    tertiary = Color(0xFF2FB57D),
    background = Color(0xFFF2FAFE),       // 浅青白 bg-primary
    onBackground = Color(0xFF0A2A3A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A2A3A),
    surfaceVariant = Color(0xFFEAF6FB),
    onSurfaceVariant = Color(0xFF4E7184),
    outline = Color(0xFFD4E8F1),
    error = Color(0xFFE5484D),
    onError = Color(0xFFFFFFFF),
)

// 圆角形状：统一接入 WhaleRadius（规范第五节），大圆角更具包裹感
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(WhaleRadius.XS),
    small = RoundedCornerShape(WhaleRadius.SM),
    medium = RoundedCornerShape(WhaleRadius.MD),
    large = RoundedCornerShape(WhaleRadius.LG),
    extraLarge = RoundedCornerShape(WhaleRadius.XL),
)

@Composable
fun SurfReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content,
    )
}

// ─────────────────────────────────────────────
// 阅读器专用配色（独立于应用主题，阅读器内自行控制）—— 对齐画布海洋流动
// ─────────────────────────────────────────────

object ReaderColors {
    // 日间（暖纸色，画布 #FBF7EF，纸质感）
    val DayBackground = Color(0xFFFBF7EF)
    val DayText = Color(0xFF2A2622)

    // 护眼绿
    val EyeGreenBackground = Color(0xFFCCE8CF)
    val EyeGreenText = Color(0xFF1E3323)

    // 夜间（深海洋蓝黑，画布 #04141F）
    val NightBackground = Color(0xFF04141F)
    val NightText = Color(0xFFCFE8F2)

    // 纯黑（OLED）
    val BlackBackground = Color(0xFF000000)
    val BlackText = Color(0xFF9AA0A6)
}

/** 阅读主题定义（下标与 ReaderSettings.themeIndex 对应） */
data class ReaderTheme(
    val name: String,
    val background: Color,
    val text: Color,
)

val ReaderThemes = listOf(
    ReaderTheme("米纸", ReaderColors.DayBackground, ReaderColors.DayText),
    ReaderTheme("护眼绿", ReaderColors.EyeGreenBackground, ReaderColors.EyeGreenText),
    ReaderTheme("夜间", ReaderColors.NightBackground, ReaderColors.NightText),
    ReaderTheme("纯黑", ReaderColors.BlackBackground, ReaderColors.BlackText),
)

/** 阅读正文衬线字体（系统衬线，零字体文件，呈现纸质书观感） */
val ReaderBodyFont = FontFamily.Serif
