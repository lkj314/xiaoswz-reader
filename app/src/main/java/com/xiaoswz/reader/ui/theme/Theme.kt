package com.xiaoswz.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 品牌色：海洋青（呼应"冲浪"主题）
private val OceanPrimary = Color(0xFF0E7490)
private val OceanPrimaryLight = Color(0xFF67E8F9)

private val LightColors = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = Color(0xFF0891B2),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
)

private val DarkColors = darkColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = Color(0xFF083344),
    primaryContainer = Color(0xFF155E75),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
)

@Composable
fun SurfReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

// ─────────────────────────────────────────────
// 阅读器专用配色（独立于应用主题，阅读器内自行控制）
// ─────────────────────────────────────────────

object ReaderColors {
    // 日间（米纸色）
    val DayBackground = Color(0xFFFAF7F2)
    val DayText = Color(0xFF1F2937)

    // 夜间
    val NightBackground = Color(0xFF0B0F14)
    val NightText = Color(0xFFB8C0C8)
}
