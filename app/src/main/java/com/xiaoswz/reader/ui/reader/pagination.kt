package com.xiaoswz.reader.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp

/**
 * 正文预处理：按设置应用首行缩进与段落间距
 *
 * @param paraSpacing 每段之间额外插入的空行数（0/1/2）
 */
fun preprocessContent(raw: String, indent: Boolean, paraSpacing: Int): String {
    val paragraphs = raw.split("\n")
    if (paragraphs.size == 1 && !indent) return raw
    return buildString(raw.length + paragraphs.size * 4) {
        paragraphs.forEachIndexed { index, para ->
            val trimmed = para.trim()
            if (indent && trimmed.isNotEmpty()) {
                append("　　").append(trimmed)
            } else {
                append(para)
            }
            if (index < paragraphs.lastIndex) {
                append("\n")
                repeat(paraSpacing) { append("\n") }
            }
        }
    }
}

/**
 * 文本分页：用 TextMeasurer 排版后按页高切分为若干页面
 *
 * @return 每页的文本切片；无法分页时返回空列表
 */
fun paginateText(
    text: String,
    fontSizeSp: Int,
    lineSpacingMultiplier: Float,
    maxWidthPx: Int,
    maxHeightPx: Int,
    textMeasurer: TextMeasurer,
): List<String> {
    if (text.isBlank() || maxWidthPx <= 0 || maxHeightPx <= 0) return emptyList()

    val style = TextStyle(
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
        fontFamily = FontFamily.Default,
    )

    val layout = textMeasurer.measure(
        text = AnnotatedString(text),
        style = style,
        constraints = Constraints(maxWidth = maxWidthPx, maxHeight = Constraints.Infinity),
    )

    if (layout.lineCount <= 0) return listOf(text)

    // 用首行实际行高推算每页可容纳行数
    val firstLineHeight = layout.getLineBottom(0) - layout.getLineTop(0)
    if (firstLineHeight <= 0f) return listOf(text)

    val linesPerPage = (maxHeightPx / firstLineHeight).toInt().coerceAtLeast(1)

    val pages = mutableListOf<String>()
    var startLine = 0
    while (startLine < layout.lineCount) {
        val endLineExclusive = minOf(startLine + linesPerPage, layout.lineCount)
        val startOffset = layout.getLineStart(startLine)
        val endOffset = layout.getLineEnd(endLineExclusive - 1)
        pages.add(text.substring(startOffset, endOffset))
        startLine = endLineExclusive
    }
    return pages
}
