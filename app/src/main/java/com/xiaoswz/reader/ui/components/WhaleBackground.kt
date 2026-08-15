package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.theme.WhaleColors

/**
 * 全局浅色玻璃风背景：在内容之下铺一层极浅蓝灰渐变，再点缀几枚柔和光斑
 * （浅蓝 / 浅薰衣草 / 浅薄荷），让上层磨砂玻璃卡片有可被「磨砂」的微弱色彩层次，
 * 呈现 iOS 控制中心式通透质感。纯代码绘制，不使用任何图片素材。
 *
 * 径向渐变天然向边缘淡出为透明，因此光斑无硬边，无需裁剪。
 */
@Composable
fun WhaleBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 基础渐变底
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleColors.GradientBackground),
        )
        // 左上柔光斑（浅蓝）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset((-80).dp, (-80).dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFBFD8FF).copy(alpha = 0.55f),
                            Color(0xFFBFD8FF).copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        // 右下柔光斑（浅薰衣草）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .size(380.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFD9CCFF).copy(alpha = 0.45f),
                            Color(0xFFD9CCFF).copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        // 中部偏下柔光斑（浅薄荷）
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset((-40).dp, 140.dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFC7F0E0).copy(alpha = 0.40f),
                            Color(0xFFC7F0E0).copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        content()
    }
}
