package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.theme.WhaleRadius

/**
 * 玻璃拟态卡片背景 Modifier（全局复用）。
 *
 * 实现：半透明深色表面 + 1dp 白色细边框。在深色背景 / 背景图上叠一层即呈现
 * 毛玻璃观感（Compose 无原生 backdrop-blur，半透明 + 边框已足够营造层次，
 * 真正的 RenderEffect 模糊留待 Phase 4 增强）。
 */
fun Modifier.whaleGlassCard(
    radius: Dp = WhaleRadius.LG,
    borderAlpha: Float = 0.10f,
): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(WhaleColors.GlassSurface)
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = borderAlpha),
        shape = RoundedCornerShape(radius),
    )

/** 玻璃拟态卡片容器：自带 16dp 内边距，直接放内容即可。 */
@Composable
fun WhaleGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = WhaleRadius.LG,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .whaleGlassCard(radius)
            .padding(16.dp),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

/** 胶囊形 Modifier（按钮 / 标签 / 头像） */
fun Modifier.whaleCapsule(): Modifier =
    this.clip(RoundedCornerShape(WhaleRadius.Full))
