package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.theme.WhaleRadius

/**
 * Meta 按钮体系（模板 00003）
 *  - Black   主品牌 CTA：墨黑 #0A1317 胶囊 + 白字
 *  - Cobalt  购买 / 主流程 CTA：钴蓝 #0064E0 胶囊 + 白字
 *  - Outline 次级：透明底 + 发丝边 + 墨字
 *  - Ghost   弱按钮：浅灰底 #F1F4F7 + Steel 字
 * 全胶囊（WhaleRadius.Full），15sp Medium，左右 24 / 上下 12。
 *
 * 两个入口：
 *  1) MetaButton(text, onClick, ...)            纯文字（可选前置图标）
 *  2) MetaButton(onClick, ..., content = {...}) 自定义内容（图标 + 文字等）
 */
enum class MetaButtonVariant { Black, Cobalt, Outline, Ghost }

private data class MetaButtonColors(
    val container: Color,
    val content: Color,
    val border: Color?,
)

@Composable
private fun resolveMetaColors(variant: MetaButtonVariant, enabled: Boolean): MetaButtonColors {
    val base = when (variant) {
        MetaButtonVariant.Black -> MetaButtonColors(Color(0xFF0A1317), Color.White, null)
        MetaButtonVariant.Cobalt -> MetaButtonColors(WhaleColors.OceanMid, Color.White, null)
        MetaButtonVariant.Outline -> MetaButtonColors(Color.Transparent, WhaleColors.TextPrimary, WhaleColors.GlassBorder)
        MetaButtonVariant.Ghost -> MetaButtonColors(Color(0xFFF1F4F7), WhaleColors.TextSecondary, null)
    }
    return if (enabled) {
        base
    } else {
        MetaButtonColors(Color(0xFFEDEFF1), Color(0xFF9AA0A6), null)
    }
}

@Composable
private fun MetaButtonShell(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: MetaButtonVariant,
    enabled: Boolean,
    contentColor: Color,
    content: @Composable RowScope.() -> Unit,
) {
    val (container, _, border) = resolveMetaColors(variant, enabled)
    val shape = RoundedCornerShape(WhaleRadius.Full)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(container)
                .let { m -> if (variant == MetaButtonVariant.Outline && enabled && border != null) m.border(1.dp, border, shape) else m }
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                content()
            }
        }
    }
}

/** 纯文字按钮（可选前置图标） */
@Composable
fun MetaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    variant: MetaButtonVariant = MetaButtonVariant.Black,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = resolveMetaColors(variant, enabled)
    MetaButtonShell(onClick, modifier, variant, enabled, colors.content) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, color = colors.content, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

/** 自定义内容按钮（图标 + 文字等任意 Row 内容） */
@Composable
fun MetaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    variant: MetaButtonVariant = MetaButtonVariant.Black,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = resolveMetaColors(variant, enabled)
    MetaButtonShell(onClick, modifier, variant, enabled, colors.content, content)
}
