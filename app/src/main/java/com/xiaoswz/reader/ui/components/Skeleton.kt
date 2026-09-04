package com.xiaoswz.reader.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors

/**
 * Meta 骨架屏体系（0.20.0）
 *
 * 旧实现有两个问题：
 * 1. 流光渐变的 start/end 用的是 0..1 的**像素**坐标，高光带只有 1px，肉眼几乎看不见；
 * 2. 高光色是 base 自身降 alpha（更透明 → 更暗），方向反了，越流越脏。
 *
 * 现在：在 drawBehind 里按真实像素尺寸计算一条 55% 宽的斜向高光带，
 * 高光恒为白色叠加（浅色 0.78 / 深色 0.10，按底色亮度自动切换）。
 */

/** 骨架基础色：Meta 柔云灰 */
private val skeletonBase: Color
    @Composable
    get() = WhaleColors.Foam

/** 高光色：按底色亮度自适应，浅色强高光、深色弱高光 */
private fun skeletonHighlight(base: Color): Color =
    if (base.luminance() < 0.5f) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.78f)

/**
 * 全 App 共享的流光进度（0.20.4 性能修复）。
 *
 * 旧实现在每个骨架 cell 内各自 `rememberInfiniteTransition()`：书城/书架一屏几十张封面
 * 就会同时跑几十个独立的无限动画，封面还在加载时整屏持续重绘，既卡顿又让人觉得
 * 「一直在加载」。现在由 AppRoot 提供**一个**共享进度，所有骨架一起流光，
 * 动画实例从 N 个降到 1 个。
 *
 * 未提供时（例如独立预览）自动回退为各 cell 自建动画，行为与旧版一致。
 */
val LocalShimmerProgress = compositionLocalOf<State<Float>?> { null }

/**
 * 给任意容器套上流光。必须在已设定尺寸的容器上使用（内部按像素计算高光带）。
 */
@Composable
fun Modifier.skeletonShimmer(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val shared = LocalShimmerProgress.current
    val progress = if (shared != null) {
        // 共享模式：不再自建动画，直接读取 AppRoot 提供的那一个进度
        shared.value
    } else {
        val transition = rememberInfiniteTransition(label = "skeletonShimmer")
        val local by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
            ),
            label = "skeletonShimmerProgress",
        )
        local
    }
    val base = skeletonBase
    val highlight = skeletonHighlight(base)
    return this.drawBehind {
        val band = size.width * 0.55f
        if (band <= 0f || size.height <= 0f) return@drawBehind
        val x = -band + progress * (size.width + band * 2f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(x, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

/** 骨架原子：一个带流光的圆角块 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusSM),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .skeletonShimmer(),
    )
}

/** 骨架文字条：[widthFraction] 为占父容器宽度的比例 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 12.dp,
) {
    Box(modifier.fillMaxWidth(widthFraction.coerceIn(0.05f, 1f))) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        )
    }
}

/**
 * 封面骨架（2:3 + 标题两行）。书城 / 书架 / 书单网格加载态统一用这个。
 */
@Composable
fun BookCoverSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = RoundedCornerShape(GlassTokens.RadiusXL),
        )
        Spacer(Modifier.height(8.dp))
        SkeletonText(widthFraction = 0.85f, height = 12.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonText(widthFraction = 0.5f, height = 10.dp)
    }
}

/** 横向列表项骨架（64×96 缩略图 + 标题 / 副标题） */
@Composable
fun RowSkeleton(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        SkeletonBox(
            modifier = Modifier.size(64.dp, 96.dp),
            shape = RoundedCornerShape(GlassTokens.RadiusMD),
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonText(widthFraction = 0.7f, height = 16.dp)
            Spacer(Modifier.height(2.dp))
            SkeletonText(widthFraction = 0.45f, height = 12.dp)
            Spacer(Modifier.height(2.dp))
            SkeletonText(widthFraction = 0.28f, height = 10.dp)
        }
    }
}

/** 信息流条目骨架（圆形头像 + 三行文本） */
@Composable
fun FeedItemSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonText(widthFraction = 0.32f, height = 12.dp)
                SkeletonText(widthFraction = 0.2f, height = 9.dp)
            }
        }
        Spacer(Modifier.height(10.dp))
        SkeletonText(widthFraction = 1f, height = 12.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonText(widthFraction = 0.82f, height = 12.dp)
    }
}

/** 详情头部骨架（大封面 + 标题 + 元信息 + 按钮） */
@Composable
fun DetailHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        SkeletonBox(
            modifier = Modifier.size(96.dp, 144.dp),
            shape = RoundedCornerShape(GlassTokens.RadiusLG),
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonText(widthFraction = 0.9f, height = 20.dp)
            Spacer(Modifier.height(2.dp))
            SkeletonText(widthFraction = 0.5f, height = 12.dp)
            SkeletonText(widthFraction = 0.35f, height = 12.dp)
            SkeletonText(widthFraction = 0.6f, height = 12.dp)
        }
    }
}

/** 卡片骨架（白卡 + 标题 + 三段正文），用于书圈 / 统计 / 设置分组 */
@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        SkeletonText(widthFraction = 0.45f, height = 14.dp)
        Spacer(Modifier.height(12.dp))
        SkeletonText(widthFraction = 1f, height = 10.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonText(widthFraction = 0.94f, height = 10.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonText(widthFraction = 0.6f, height = 10.dp)
    }
}
