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

// 品牌色：海洋青（呼应"冲浪"主题），M3 Expressive 更鲜艳、对比更强
private val OceanPrimary = Color(0xFF0E7490)
private val OceanPrimaryLight = Color(0xFF22D3EE)
private val OceanSecondary = Color(0xFF0891B2)
private val OceanTertiary = Color(0xFF0D9488)

private val LightColors = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF0B3A47),
    secondary = OceanSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2ECF5),
    onSecondaryContainer = Color(0xFF06455A),
    tertiary = OceanTertiary,
    background = Color(0xFFF6FAFB),
    surface = Color(0xFFFDFDFD),
    surfaceVariant = Color(0xFFE3EDF1),
    onSurfaceVariant = Color(0xFF45586A),
    outline = Color(0xFFC3D3DC),
)

private val DarkColors = darkColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = Color(0xFF003844),
    primaryContainer = Color(0xFF0B5566),
    onPrimaryContainer = Color(0xFFA8EEF8),
    secondary = Color(0xFF5BD0E6),
    onSecondary = Color(0xFF003542),
    secondaryContainer = Color(0xFF044C5C),
    onSecondaryContainer = Color(0xFFA7E6F3),
    tertiary = Color(0xFF4FD1C5),
    background = Color(0xFF0A1118),
    surface = Color(0xFF121B24),
    surfaceVariant = Color(0xFF22303D),
    onSurfaceVariant = Color(0xFFB6C6D2),
    outline = Color(0xFF34444F),
)

// 圆角形状：M3 Expressive —— 大圆角，卡片更具包裹感
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
// 阅读器专用配色（独立于应用主题，阅读器内自行控制）
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
