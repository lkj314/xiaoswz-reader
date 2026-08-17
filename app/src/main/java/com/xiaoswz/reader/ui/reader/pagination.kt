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

/**
 * 单个非空段落（去空行后）在「预处理后正文」中的区间信息。
 * 段评锚定以 [index]（去空行段落序号，Raw 坐标）为主键，[contentStartInSeg]/[contentLength]
 * 描述该段 trim 后正文在预处理段内的偏移，与阅读设置（缩进/段距）解耦，跨设备一致。
 */
data class ParaRange(
    val index: Int,
    val processedStart: Int,
    val contentStartInSeg: Int,
    val contentLength: Int,
    val contentText: String,
)

/**
 * 将 Raw 正文按与 [preprocessContent] 完全相同的规则，计算出每个非空段落
 * 在预处理后字符串中的起始位置与段内内容区间。用于把段评锚点映射回渲染坐标。
 */
fun mapParagraphRanges(raw: String, indent: Boolean, paraSpacing: Int): List<ParaRange> {
    val rawLines = raw.split("\n")
    val ranges = mutableListOf<ParaRange>()
    var pos = 0
    var paraIndex = 0
    rawLines.forEachIndexed { i, line ->
        val trimmed = line.trim()
        val isEmpty = trimmed.isEmpty()
        // 与 preprocessContent 保持一致：缩进时对非空段前置全角两格，否则保留原行
        val seg = if (indent && !isEmpty) "　　$trimmed" else line
        if (!isEmpty) {
            val contentStartInSeg = seg.indexOf(trimmed).coerceAtLeast(0)
            ranges.add(
                ParaRange(
                    index = paraIndex,
                    processedStart = pos,
                    contentStartInSeg = contentStartInSeg,
                    contentLength = trimmed.length,
                    contentText = trimmed,
                ),
            )
            paraIndex++
        }
        pos += seg.length
        if (i < rawLines.lastIndex) {
            // 段间分隔：1 个换行 + paraSpacing 个空行（与 preprocessContent 一致）
            pos += 1 + paraSpacing
        }
    }
    return ranges
}

/** 段评锚点（段落序号 + 段内偏移 + 引用快照） */
data class SegmentAnchor(
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val quotedText: String,
)

/**
 * 由预处理正文中的选中区间 [selStart, selEnd) 反算段评锚点。
 * 返回包含选区起点的段落序号，以及相对该段 trim 正文的偏移；quotedText 取选中文本。
 * 选区跨段时以起点所在段为准。无有效段落时返回 null。
 */
fun anchorFromSelection(
    ranges: List<ParaRange>,
    selStart: Int,
    selEnd: Int,
): SegmentAnchor? {
    if (selEnd <= selStart) return null
    val range = ranges.firstOrNull { r ->
        val cStart = r.processedStart + r.contentStartInSeg
        val cEnd = cStart + r.contentLength
        selStart >= cStart && selStart < cEnd
    } ?: return null
    val cStart = range.processedStart + range.contentStartInSeg
    val start = (selStart - cStart).coerceIn(0, range.contentLength)
    val end = (selEnd - cStart).coerceIn(start, range.contentLength)
    if (end <= start) return null
    val quoted = range.contentText.substring(start, end)
    return SegmentAnchor(range.index, start, end, quoted)
}

/** 将段内偏移区间映射回预处理正文中的绝对字符区间（用于下划线渲染）。 */
fun segmentAbsoluteSpan(
    range: ParaRange,
    contentStart: Int,
    contentEnd: Int,
): Pair<Int, Int> {
    val base = range.processedStart + range.contentStartInSeg
    return (base + contentStart.coerceAtLeast(0)) to (base + contentEnd.coerceAtMost(range.contentLength))
}

/**
 * 内容寻址落锚（v0.15.1）：给定章节原始正文与用户选中的「引用文本」，
 * 在各段落 trim 后的 [ParaRange.contentText] 中查找该引用（容忍首尾全角缩进与空白差异），
 * 返回与 [anchorFromSelection] / 渲染完全一致的 [SegmentAnchor]。
 *
 * 设计要点：不依赖段落序号的稳定性——只要那段话还在正文里，就能正确归位。
 * 这直接回应「段评锚定过于乐观」的担忧：书源正文即使段落重排 / 微调，
 * 段评依然能靠引用文本本身定位（契合正文不入库的隔离设计）。
 * 连续滚动模式下选区只能拿到选中文字（拿不到数字偏移），本函数正是该模式的落锚手段。
 *
 * @param quote 用户选中的文本（可能来自预处理后的显示文本）
 * @return 命中段落的锚点；未命中返回 null
 */
fun locateQuote(raw: String, quote: String, indent: Boolean, paraSpacing: Int): SegmentAnchor? {
    val q = quote.trim()
    if (q.isEmpty()) return null
    val ranges = mapParagraphRanges(raw, indent, paraSpacing)
    val nQ = normalizeWs(q)
    for (r in ranges) {
        val ct = r.contentText
        // 1) 直接子串匹配（最常见：选中文本恰为某段落内子串）
        var idx = ct.indexOf(q)
        if (idx >= 0) return makeAnchor(r, idx, idx + q.length, ct)
        // 2) 去掉用户可能连选的首行全角/半角缩进后再试
        val q2 = q.removePrefix("　　").removePrefix(" ").trim()
        if (q2 != q) {
            idx = ct.indexOf(q2)
            if (idx >= 0) return makeAnchor(r, idx, idx + q2.length, ct)
        }
        // 3) 空白归一化兜底（容忍预处理缩进/段距与源的差异）
        val nIdx = normalizeWs(ct).indexOf(nQ)
        if (nIdx >= 0) {
            val (s, e) = mapNormToRaw(normalizeWs(ct), ct, nIdx, nIdx + nQ.length)
            if (s >= 0 && e > s) return makeAnchor(r, s, e, ct)
        }
    }
    return null
}

private fun makeAnchor(r: ParaRange, start: Int, end: Int, ct: String): SegmentAnchor {
    val s = start.coerceAtLeast(0)
    val e = end.coerceAtMost(ct.length)
    if (e <= s) return SegmentAnchor(r.index, 0, ct.length, ct)
    return SegmentAnchor(r.index, s, e, ct.substring(s, e))
}

/** 折叠所有空白（含全角空格/换行）为单空格并 trim，用于容错匹配 */
private fun normalizeWs(s: String): String = s.replace(Regex("\\s+"), " ").trim()

/**
 * 将「空白归一化」坐标映射回原始 contentText 坐标。
 * 归一化串与原始串的非空白字符一一对应，故按非空白字符计数即可定位。
 */
private fun mapNormToRaw(norm: String, raw: String, nStart: Int, nEnd: Int): Pair<Int, Int> {
    var ni = 0
    var rs = -1
    var re = -1
    for (ri in raw.indices) {
        val c = raw[ri]
        val isWs = c.isWhitespace() || c == '　'
        if (!isWs) {
            if (ni == nStart) rs = ri
            if (ni == nEnd - 1) {
                re = ri + 1
                break
            }
            ni++
        }
    }
    if (rs < 0) rs = 0
    if (re < 0) re = raw.length
    return rs to re
}

