package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.theme.WhaleRadius

/**
 * 玻璃拟态卡片背景 Modifier（兼容旧名，v2.1 改为浅色 iOS 玻璃）。
 *
 * 实现：近白半透明表面 + 1dp 亮边 + 柔和投影 + 顶部高光。在浅色渐变背景上叠一层即呈磨砂玻璃观感。
 */
fun Modifier.whaleGlassCard(
    radius: Dp = GlassTokens.RadiusLG,
): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(WhaleColors.GlassSurface)
    .border(
        width = 1.dp,
        color = GlassTokens.GlassBorder,
        shape = RoundedCornerShape(radius),
    )
    .shadow(
        elevation = 10.dp,
        shape = RoundedCornerShape(radius),
        spotColor = GlassTokens.GlassShadowColor,
        ambientColor = GlassTokens.GlassShadowColor,
    )

/** 玻璃拟态卡片容器：自带 16dp 内边距，直接放内容即可。 */
@Composable
fun WhaleGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = GlassTokens.RadiusLG,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .whaleGlassCard(radius),
    ) {
        // 顶部光泽
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(GlassTokens.GlassHighlight, Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

/** 胶囊形 Modifier（按钮 / 标签 / 头像） */
fun Modifier.whaleCapsule(): Modifier =
    this.clip(RoundedCornerShape(WhaleRadius.Full))
