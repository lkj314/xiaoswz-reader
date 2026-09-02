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
// 冲浪阅读 设计系统：Meta（模板 00003）
//   白底灰边 · 钴蓝 #0064E0 主流程/购买 · 墨阶文字 · 语义成功/危险/注意
//   圆角紧凑（xs2 · sm4 · md8 · lg12 · xl16 · full）
// 设计令牌（色板 / 圆角）与画布设计稿「冲浪阅读-Meta设计升级」严格对齐。
// ─────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2E90FA),          // 深底上更亮的钴蓝，保证对比
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1C5FBA),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF5D6C7B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF31A24C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0A1317),       // Ink Deep
    onBackground = Color(0xFFEAF0F6),
    surface = Color(0xFF18191A),
    onSurface = Color(0xFFE4E6EB),
    surfaceVariant = Color(0xFF242526),    // 底栏 / 浮层
    onSurfaceVariant = Color(0xFFB0B8C1),
    outline = Color(0xFF3A3B3C),
    outlineVariant = Color(0xFF2C2D2E),
    error = Color(0xFFFF5C75),
    onError = Color(0xFF0A1317),
)

// 浅色（默认）：白底 + 钴蓝强调 + 墨阶文字
private val LightColors = lightColorScheme(
    primary = Color(0xFF0064E0),          // 钴蓝：CTA / 选中态 / 链接 / 购买
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7F0FF),
    onPrimaryContainer = Color(0xFF0457CB),
    secondary = Color(0xFF5D6C7B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF31A24C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),        // Canvas 白
    onBackground = Color(0xFF0A1317),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1E21),        // Ink
    surfaceVariant = Color(0xFFF1F4F7),    // 表面柔云 / 分组背景
    onSurfaceVariant = Color(0xFF5D6C7B),
    outline = Color(0xFFCED0D4),          // 发丝边
    outlineVariant = Color(0xFFE3E6EA),
    error = Color(0xFFE41E3F),            // Critical
    onError = Color(0xFFFFFFFF),
)

// 圆角形状：统一接入 Meta 紧凑圆角（规范：xs2 · sm4 · md8 · lg12 · xl16）
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
// 阅读器专用配色（独立于应用主题，阅读器内自行控制）—— 对齐画布米纸阅读
// ─────────────────────────────────────────────

object ReaderColors {
    // 日间（暖纸色，画布 #FAF7EE，纸质感）
    val DayBackground = Color(0xFFFAF7EE)
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
