package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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

/**
 * 苹果玻璃（Liquid Glass · 浅色）材质 Modifier。
 *
 * 实现：近白半透明填充 + 柔和投影 + 1dp 亮边高光。纯代码绘制，无任何图片素材。
 * 在浅色渐变背景之上叠一层即呈现 iOS 控制中心式磨砂玻璃观感。
 *
 * @param radius     圆角（iOS 偏大）
 * @param fillAlpha  填充不透明度（越高越实，越低越透）
 * @param withShadow 是否带柔和投影（浮层 / 卡片建议 true）
 */
fun Modifier.liquidGlass(
    radius: Dp = GlassTokens.RadiusLG,
    fillAlpha: Float = 0.62f,
    withShadow: Boolean = true,
): Modifier {
    val base = this
        .clip(RoundedCornerShape(radius))
        .background(GlassTokens.GlassFill.copy(alpha = fillAlpha))
        .border(
            width = 1.dp,
            color = GlassTokens.GlassBorder,
            shape = RoundedCornerShape(radius),
        )
    return if (withShadow) {
        base.shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(radius),
            spotColor = GlassTokens.GlassShadowColor,
            ambientColor = GlassTokens.GlassShadowColor,
        )
    } else {
        base
    }
}

/**
 * 玻璃卡片容器：自带顶部光泽叠层 + 默认 16dp 内边距。
 * 直接放内容即可，自动获得磨砂玻璃质感。
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = GlassTokens.RadiusLG,
    fillAlpha: Float = 0.62f,
    withShadow: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .liquidGlass(radius, fillAlpha, withShadow),
    ) {
        // 顶部光泽：从顶边向内渐隐的白色高光（光线打在玻璃上）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(GlassTokens.GlassHighlight, Color.Transparent),
                    ),
                ),
        )
        // 底部折射高光：从底边向内渐隐的白色（光线穿过玻璃底部，iOS 液体玻璃招牌）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GlassTokens.GlassHighlight.copy(alpha = 0.35f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/** 胶囊形 Modifier（按钮 / 标签 / 头像） */
fun Modifier.glassCapsule(): Modifier =
    this.clip(RoundedCornerShape(GlassTokens.RadiusPill))
