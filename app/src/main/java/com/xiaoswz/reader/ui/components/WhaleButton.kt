package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 */
enum class MetaButtonVariant { Black, Cobalt, Outline, Ghost }

@Composable
fun MetaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    variant: MetaButtonVariant = MetaButtonVariant.Black,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(WhaleRadius.Full)
    val (containerColor, contentColor, borderColor) = when (variant) {
        MetaButtonVariant.Black -> Triple(Color(0xFF0A1317), Color.White, null)
        MetaButtonVariant.Cobalt -> Triple(WhaleColors.OceanMid, Color.White, null)
        MetaButtonVariant.Outline -> Triple(Color.Transparent, WhaleColors.TextPrimary, WhaleColors.GlassBorder)
        MetaButtonVariant.Ghost -> Triple(Color(0xFFF1F4F7), WhaleColors.TextSecondary, null)
    }
    val (cc, tc) = if (enabled) {
        Pair(containerColor, contentColor)
    } else {
        Pair(Color(0xFFEDEFF1), Color(0xFF9AA0A6))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(cc)
            .let { m -> if (variant == MetaButtonVariant.Outline && enabled && borderColor != null) m.border(1.dp, borderColor, shape) else m }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = tc, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}
