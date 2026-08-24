package com.xiaoswz.reader.ui.reader

// 本文件从 ReaderScreen.kt 拆分而来（P2 大文件拆分）：承载阅读器正文的标注/搜索/分享/
// 听书选区浮层等「纯展示型」辅助 Composable 与数据类。原文件仅保留核心 ReaderScreen
// 组合体与内联业务逻辑，调用点（同包）通过 internal 可见性访问本文件符号。
// 注意：OverlayBg/OverlayText/OverlayTextDim/MenuCardRadius 四个常量为历史遗留死代码，
// 已被 ReaderMenuBar / AnnoSelectionBar / ReaderSelectionToolbar 改用 MaterialTheme 取代，故不迁移。

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.community.CommunityRepository
import com.xiaoswz.reader.data.plugin.DecoratorCap
import com.xiaoswz.reader.data.plugin.PluginManifest
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.launch

@Composable
internal fun SegmentMarkedText(
    text: String,
    annotations: List<AnnotationEntity>,
    readingRange: IntRange?,
    theme: ReaderTheme,
    fontSizeSp: Int,
    lineSpacing: Float,
    decorators: Map<String, DecoratorCap> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val annotated = remember(text, annotations, readingRange, theme, decorators) {
        buildAnnotatedContent(text, annotations, readingRange, theme, decorators)
    }
    SelectionContainer {
        Text(
            text = annotated,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineSpacing).sp,
            color = theme.text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ChapterBlockView(
    block: ChapterBlock,
    theme: ReaderTheme,
    settings: ReaderSettings,
    isReadingChapter: Boolean = false,
    readingRange: IntRange? = null,
    annotations: List<AnnotationEntity> = emptyList(),
    decorators: Map<String, DecoratorCap> = emptyMap(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp),
    ) {
        Text(
            text = block.title,
            style = MaterialTheme.typography.titleLarge,
            color = theme.text,
        )
        if (block.index >= 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "第 ${block.index + 1} 章",
                style = MaterialTheme.typography.bodySmall,
                color = theme.text.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        SegmentMarkedText(
            text = block.content,
            annotations = annotations,
            readingRange = if (isReadingChapter) readingRange else null,
            theme = theme,
            fontSizeSp = settings.fontSize,
            lineSpacing = settings.lineSpacing,
            decorators = decorators,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

/** 书内搜索面板 */
@Composable
internal fun SearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchMatch>,
    searching: Boolean,
    onResultClick: (SearchMatch) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("书内搜索", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("输入关键词") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (searching) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else if (results.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("未找到「$query」", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(results, key = { "${it.chapterId}:${it.offset}" }) { m ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(m) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(m.chapterTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("…${m.snippet}…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

/**
 * 分享当前书摘到书友圈（模块D）：预填本章标题+正文，可编辑后发布。
 * 复用 CommunityRepository.createPost（需登录；未登录由后端返回 login_required 并映射提示）。
 */
@Composable
internal fun ReaderShareSheet(
    initialText: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetScope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText) }
    var posting by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("分享到社区", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 300.dp),
            label = { Text("书摘内容") },
            singleLine = false,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val content = text.trim()
                if (content.isEmpty()) {
                    android.widget.Toast.makeText(context, "内容不能为空", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }
                posting = true
                sheetScope.launch {
                    CommunityRepository.createPost(content, emptyList())
                        .onSuccess {
                            android.widget.Toast.makeText(context, "已分享到社区", android.widget.Toast.LENGTH_SHORT).show()
                            posting = false
                            onDismiss()
                        }
                        .onFailure { e ->
                            android.widget.Toast.makeText(context, e.message ?: "分享失败（需登录）", android.widget.Toast.LENGTH_SHORT).show()
                            posting = false
                        }
                }
            },
            enabled = !posting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (posting) "发布中…" else "发布到社区")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** 一句在原文中的区间，用于听书锚点高亮与 TTS 索引同步 */
internal data class SentenceRange(val text: String, val start: Int, val end: Int)

/**
 * 将正文 + 既有标注 + TTS 朗读区间合并为带背景样式的 AnnotatedString。
 * 标注锚点（decorator 槽）与听书高亮同源渲染，互不干扰；标注偏移相对预处理正文。
 * 同时被本文件 SegmentMarkedText 与 ReaderScreen 核心组合体调用，故为 internal。
 */
internal fun buildAnnotatedContent(
    content: String,
    annotations: List<AnnotationEntity>,
    readingRange: IntRange?,
    theme: ReaderTheme,
    decorators: Map<String, DecoratorCap> = emptyMap(),
): AnnotatedString = buildAnnotatedString {
    append(content)
    for (a in annotations) {
        val s = a.startOffset.coerceAtLeast(0)
        val e = a.endOffset.coerceAtMost(content.length)
        if (e > s) {
            // decorator 槽优先：若该标注类型挂了「下划线」类装饰插件（如官方红批注下划线），
            // 则按装饰插件的样式渲染，让创意工坊 decorator 能力真正生效。
            val deco = decorators[a.type]
            if (deco != null && deco.style == "underline") {
                val c = (deco.color ?: a.color)?.let { Color(it) } ?: Color(-65536)
                addStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = c,
                        background = c.copy(alpha = 0.08f),
                    ),
                    s,
                    e,
                )
            } else {
                when (a.type) {
                    // 书签：冷色下划线 + 极淡底，与「高亮」（暖色块）明确区分，避免二者混淆
                    "bookmark" -> {
                        val c = a.color?.let { Color(it) } ?: Color(0xFF3B82F6)
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = c,
                                background = c.copy(alpha = 0.08f),
                            ),
                            s,
                            e,
                        )
                    }
                    // 高亮（及其余未知类型）：暖色块背景，沿用既有观感
                    else -> {
                        val c = a.color?.let { Color(it) } ?: Color(-14336)
                        addStyle(SpanStyle(background = c.copy(alpha = 0.22f)), s, e)
                    }
                }
            }
        }
    }
    if (readingRange != null) {
        val s = readingRange.first.coerceAtLeast(0)
        val e = readingRange.last + 1
        if (e <= content.length && e > s) {
            addStyle(SpanStyle(background = theme.text.copy(alpha = 0.14f)), s, e)
        }
    }
}

/**
 * 划词标注选区浮层：只读 BasicTextField 选中文字后浮现，提供「复制」与动态插件动作
 * （高亮/书签等）。仅 single-chapter 滚动模式可用（由 [annoAvailable] 收敛）。
 */
@Composable
internal fun AnnoSelectionBar(
    visible: Boolean,
    plugins: List<PluginManifest>,
    onCopy: () -> Unit,
    onListen: () -> Unit,
    onAnnotate: (PluginManifest) -> Unit,
    onComment: () -> Unit = {},
    onClose: () -> Unit,
) {
    if (!visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCopy) { Text("复制") }
            TextButton(onClick = onListen) { Text("听书") }
            for (plugin in plugins) {
                val cap = plugin.capabilities.annotation ?: continue
                TextButton(onClick = { onAnnotate(plugin) }) { Text(cap.label) }
            }
            TextButton(onClick = onComment) { Text("评") }
            TextButton(onClick = onClose) { Text("完成") }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 听书：选中文字起点 相关（自定义选区菜单 + 公开 API 拿选中文字）
// ─────────────────────────────────────────────────────────────

/**
 * 替换默认选区菜单的自定义 TextToolbar：选区出现时记录选区矩形并暴露回调，
 * 由 ReaderSelectionToolbar 在 Compose 层绘制「复制 / 全选 / 听书」浮条。
 * 参考主流阅读软件（微信读书/微信）：选中文字 → 弹出菜单 → 从选中处朗读。
 */
internal class ReaderTextToolbar : TextToolbar {
    val rectState = mutableStateOf<Rect?>(null)
    var onCopy: (() -> Unit)? = null
    var onSelectAll: (() -> Unit)? = null
    var onListen: (() -> Unit)? = null
    var onComment: (() -> Unit)? = null

    override val status: TextToolbarStatus
        get() = if (rectState.value != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        onCopy = onCopyRequested
        onSelectAll = onSelectAllRequested
        rectState.value = rect
    }

    override fun hide() {
        rectState.value = null
    }
}

/**
 * 选区浮动工具条：复制 / 全选 / 听书 / 评。定位在选区矩形上方（空间不足则置于下方）。
 * 「评」入口覆盖所有阅读模式（连续/翻页/单章滚动），点按后由 readerToolbar.onComment
 * 经剪贴板取选中文字 → 内容寻址落锚 → 弹段评发表框，回应「段评工具条不出现」的问题。
 */
@Composable
internal fun ReaderSelectionToolbar(
    rect: Rect,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onListen: () -> Unit,
    onComment: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = remember { mutableStateOf(0) }
    val barH = with(density) { 44.dp.toPx() }
    val centerX = (rect.left + rect.right) / 2f
    val y = if (rect.top > barH + 24f) rect.top - barH - 12f else rect.bottom + 12f
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { androidx.compose.ui.unit.IntOffset((centerX - widthPx.value / 2f).toInt(), y.toInt()) }
                .onSizeChanged { widthPx.value = it.width }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCopy) { Text("复制") }
            TextButton(onClick = onSelectAll) { Text("全选") }
            TextButton(onClick = onListen) { Text("听书") }
            TextButton(onClick = onComment) { Text("评") }
        }
    }
}
