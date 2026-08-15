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
// 冲浪阅读 v2.0 主题：深海陪伴（Ocean）
// 视觉定义见 VISUAL-SPEC-v2.0.md
//   DARK = 沉浸深海（品牌默认）
//   LIGHT = 日间海洋变体（保持「浅色」主题切换可用）
// ─────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B9FDA),          // whale-blue：CTA / 选中态 / 强调
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E4A8A), // whale-navy
    onPrimaryContainer = Color(0xFFCDE4F7),
    secondary = Color(0xFF8BB8E8),
    onSecondary = Color(0xFF0D1B2A),
    secondaryContainer = Color(0xFF16344A),
    onSecondaryContainer = Color(0xFFCDE4F7),
    tertiary = Color(0xFF4FD1A5),         // success-mint（语义强调备用）
    onTertiary = Color(0xFF06281D),
    background = Color(0xFF142836),       // ocean-base
    onBackground = Color(0xFFE8F0F4),
    surface = Color(0xFF1E3848),          // ocean-surface
    onSurface = Color(0xFFE8F0F4),
    surfaceVariant = Color(0xFF2A4656),   // ocean-elevated（底栏 / 浮层）
    onSurfaceVariant = Color(0xFF9AB8C8),
    outline = Color(0xFF3A5670),
    outlineVariant = Color(0xFF2A4656),
    error = Color(0xFFE8636A),
    onError = Color(0xFFFFFFFF),
)

// 日间海洋变体：浅蓝白底 + 深蓝字 + whale-blue 强调，保持切换有意义
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E4A8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4F2),
    onPrimaryContainer = Color(0xFF0B2A4A),
    secondary = Color(0xFF5B9FDA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EEF9),
    onSecondaryContainer = Color(0xFF0B3A5C),
    tertiary = Color(0xFF2E9E78),
    background = Color(0xFFEAF2F7),
    onBackground = Color(0xFF142836),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF142836),
    surfaceVariant = Color(0xFFD6E4EE),
    onSurfaceVariant = Color(0xFF4A6A7A),
    outline = Color(0xFFC3D3DC),
    error = Color(0xFFC0392B),
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}

// ─────────────────────────────────────────────
// 阅读器专用配色（独立于应用主题，阅读器内自行控制）—— 不动
// ─────────────────────────────────────────────

object ReaderColors {
    // 日间（米纸色，偏暖的米黄，更有纸质感）
    val DayBackground = Color(0xFFF3EAD8)
    val DayText = Color(0xFF1F2937)

    // 护眼绿
    val EyeGreenBackground = Color(0xFFCCE8CF)
    val EyeGreenText = Color(0xFF1E3323)

    // 夜间
    val NightBackground = Color(0xFF0B0F14)
    val NightText = Color(0xFFB8C0C8)

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
