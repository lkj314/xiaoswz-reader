package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.data.api.PricePointDto
import com.xiaoswz.reader.ui.theme.GlassTokens

/**
 * 简易 K 线 / 价格走势图（0.18 书币交易所）：基于 Canvas 绘制价格折线 + 面积填充。
 * 不依赖第三方图表库，保持包体精简。
 */
@Composable
fun CoinPriceChart(
    points: List<PricePointDto>,
    modifier: Modifier = Modifier,
    lineColor: Color = GlassTokens.SystemBlue,
) {
    val h = 160.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(h),
    ) {
        val w = size.width
        val pad = 8.dp.toPx()
        val top = pad
        val bottom = size.height - pad
        val usableH = bottom - top
        if (points.isEmpty()) {
            // 空图：画一条基线
            drawLine(Color(0xFFD0D5DD), Offset(pad, bottom), Offset(w - pad, bottom), strokeWidth = 1.dp.toPx())
            return@Canvas
        }
        val prices = points.map { it.price.toFloat() }
        val min = prices.minOrNull() ?: 0f
        val max = prices.maxOrNull() ?: 1f
        val range = if (max - min < 0.0001f) 1f else max - min
        val stepX = if (points.size <= 1) 0f else (w - 2 * pad) / (points.size - 1)

        val path = Path()
        val fillPath = Path()
        points.forEachIndexed { i, p ->
            val x = pad + i * stepX
            val y = bottom - ((p.price.toFloat() - min) / range) * usableH
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, bottom)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(pad + (points.size - 1) * stepX, bottom)
        fillPath.close()

        // 面积填充
        drawPath(fillPath, lineColor.copy(alpha = 0.12f))
        // 折线
        drawPath(path, lineColor, style = Stroke(2.dp.toPx()))

        // 末点标记
        val lastX = pad + (points.size - 1) * stepX
        val lastY = bottom - ((prices.last() - min) / range) * usableH
        drawCircle(lineColor, 4.dp.toPx(), Offset(lastX, lastY))
    }
}
