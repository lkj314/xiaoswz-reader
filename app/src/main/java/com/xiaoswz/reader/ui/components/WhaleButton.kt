package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.theme.WhaleRadius

/**
 * CTA 主按钮：whale-blue 渐变填充 + 白色文字 + 全胶囊。
 * 用于登录 / 确认 / 开始阅读等主操作。
 */
@Composable
fun WhaleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val brush = if (enabled) {
        WhaleColors.GradientButton
    } else {
        Brush.verticalGradient(listOf(Color(0xFF3A5670), Color(0xFF3A5670)))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WhaleRadius.Full))
            .background(brush = brush, shape = RoundedCornerShape(WhaleRadius.Full))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0xFF7A98A8),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
        )
    }
}
