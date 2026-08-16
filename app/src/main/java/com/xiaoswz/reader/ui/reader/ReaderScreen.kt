package com.xiaoswz.reader.ui.reader

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.CrashLogger
import com.xiaoswz.reader.data.annotation.ANNOTATION_TYPE_BOOKMARK
import com.xiaoswz.reader.data.annotation.ANNOTATION_TYPE_HIGHLIGHT
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.model.ChapterDto
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.ui.reader.components.ReaderBottomBar
import com.xiaoswz.reader.ui.reader.components.ReaderTopBar
import com.xiaoswz.reader.ui.theme.ReaderTheme
import com.xiaoswz.reader.ui.theme.ReaderThemes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 左右边距档位对应的 dp */
private val MarginDpOptions = listOf(12.dp, 20.dp, 28.dp)

/** 划线高亮调色板（ARGB） */
private val HIGHLIGHT_COLORS = listOf(
    Color(0xFFFFF176), // 黄
    Color(0xFFA5D6A7), // 绿
    Color(0xFF90CAF9), // 蓝
    Color(0xFFEF9A9A), // 红
)
private val DEFAULT_HIGHLIGHT_COLOR: Int = Color(0xFFFFF176).value.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookSlug: String,
    chapterId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = view.context
    val bookshelfRepo = remember { BookshelfRepository(context.applicationContext) }
    val scope = rememberCoroutineScope {
        CoroutineExceptionHandler { _, t -> CrashLogger.report(context, t) }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // ── 模块 A：选择 / 标注 / 搜索 的 UI 状态 ──
    var pendingSel by remember { mutableStateOf<Pair<String, TextRange>?>(null) }
    var showAnnotations by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteTargetClientId by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchMatch>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val layoutCache = remember { mutableMapOf<String, TextLayoutResult>() }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(bookSlug, chapterId) {
        viewModel.load(bookSlug, chapterId)
    }

    // ── 记录阅读进度到本地书架（仅已收藏书籍，未收藏不产生数据）──
    LaunchedEffect(state.currentChapterId) {
        val cid = state.currentChapterId
        if (!cid.isNullOrBlank()) {
            try {
                bookshelfRepo.updateProgress(bookSlug, cid, state.chapterTitle)
            } catch (e: Exception) {
                CrashLogger.report(context, e)
            }
        }
    }

    // ── 屏幕常亮 ──
    DisposableEffect(state.settings.keepScreenOn) {
        val window = (view.context as? Activity)?.window
        if (state.settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── 沉浸模式：菜单隐藏时隐藏系统栏 ──
    LaunchedEffect(state.menuVisible) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (state.menuVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? Activity)?.window ?: return@onDispose
            WindowCompat.getInsetsController(window, view)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // ── 音量键翻页 ──
    LaunchedEffect(Unit) { VolumeKeyBus.readerActive = true }
    LaunchedEffect(state.settings.volumeKeyPaging) {
        VolumeKeyBus.pagingEnabled = state.settings.volumeKeyPaging
    }
    DisposableEffect(Unit) {
        onDispose { VolumeKeyBus.readerActive = false }
    }

    val theme = ReaderThemes.getOrElse(state.settings.themeIndex) { ReaderThemes[0] }
    val hMarginDp = MarginDpOptions[state.settings.marginIndex.coerceIn(0, 2)]

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = state.menuVisible,
        drawerContent = {
            ModalDrawerSheet {
                TocDrawerContent(
                    bookName = state.bookName,
                    toc = state.toc,
                    currentChapterId = state.currentChapterId,
                    onChapterClick = { id ->
                        viewModel.goToChapter(id)
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val hMarginPx = with(density) { hMarginDp.toPx() }
            val reservedHeightPx = with(density) { 64.dp.toPx() }

            val scrollState = rememberScrollState()
            val lazyListState = rememberLazyListState()
            /** 是否处于连续滚动（无缝阅读）渲染模式 */
            val isContinuous = state.settings.continuousScroll &&
                state.settings.pageMode == ReaderSettings.MODE_SCROLL

            // 正文预处理（缩进 / 段距）
            val processed = remember(
                state.rawContent,
                state.settings.indentFirstLine,
                state.settings.paraSpacing,
            ) {
                preprocessContent(
                    state.rawContent,
                    state.settings.indentFirstLine,
                    state.settings.paraSpacing,
                )
            }

            // 选择处理：把选区（章内字符偏移）存入 pendingSel，并驱动底部工具栏
            fun handleSelection(blockId: String, sel: TextRange?) {
                if (sel == null) {
                    if (pendingSel?.first == blockId) pendingSel = null
                    return
                }
                if (sel.start != sel.end) {
                    pendingSel = blockId to sel
                } else if (pendingSel?.first == blockId) {
                    pendingSel = null
                }
            }

            // 跳转到某章（标注/书签/搜索结果命中）
            fun jumpToChapter(chapterId: String) {
                viewModel.requestScrollToChapter(chapterId)
            }

            // 导出标注为文本并分享
            fun exportAnnotations() {
                val items = state.annotations
                if (items.isEmpty()) return
                val sb = StringBuilder()
                sb.appendLine("《${state.bookName}》阅读标注导出")
                sb.appendLine()
                items.filter { it.type == ANNOTATION_TYPE_HIGHLIGHT }.forEach { a ->
                    val ch = state.toc.firstOrNull { it.id == a.chapterId }?.name ?: a.chapterId
                    sb.appendLine("【划线】$ch")
                    sb.appendLine(a.quotedText ?: "")
                    if (!a.note.isNullOrBlank()) sb.appendLine("笔记：${a.note}")
                    sb.appendLine()
                }
                items.filter { it.type == ANNOTATION_TYPE_BOOKMARK }.forEach { a ->
                    val ch = state.toc.firstOrNull { it.id == a.chapterId }?.name ?: a.chapterId
                    sb.appendLine("【书签】$ch")
                    if (!a.note.isNullOrBlank()) sb.appendLine(a.note) else sb.appendLine(a.quotedText ?: "")
                    sb.appendLine()
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                }
                context.startActivity(Intent.createChooser(intent, "导出标注"))
            }

            // 覆盖模式分页（在协程中计算，避免组合阶段同步测量整章卡顿/崩溃）
            val textMeasurer = rememberTextMeasurer()
            val pages = remember { mutableStateOf(emptyList<String>()) }
            var paginationFailed by remember { mutableStateOf(false) }
            LaunchedEffect(
                processed,
                state.settings.pageMode,
                state.settings.fontSize,
                state.settings.lineSpacing,
                widthPx,
                heightPx,
            ) {
                paginationFailed = false
                if (state.settings.pageMode != ReaderSettings.MODE_COVER) {
                    pages.value = emptyList()
                    return@LaunchedEffect
                }
                try {
                    pages.value = paginateText(
                        text = processed,
                        fontSizeSp = state.settings.fontSize,
                        lineSpacingMultiplier = state.settings.lineSpacing,
                        maxWidthPx = (widthPx - hMarginPx * 2).toInt(),
                        maxHeightPx = (heightPx - reservedHeightPx).toInt(),
                        textMeasurer = textMeasurer,
                    )
                } catch (e: Exception) {
                    CrashLogger.report(context, e)
                    paginationFailed = true
                }
            }

            val pagerState = rememberPagerState(pageCount = { pages.value.size.coerceAtLeast(1) })
            var pendingJumpToEnd by remember { mutableStateOf(false) }

            // 翻页/翻章动作（覆盖模式）
            fun coverPrev() {
                if (pagerState.currentPage > 0) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                } else {
                    pendingJumpToEnd = true
                    if (!viewModel.prevChapter()) pendingJumpToEnd = false
                }
            }

            fun coverNext() {
                if (pagerState.currentPage < pages.value.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    viewModel.nextChapter()
                }
            }

            // 翻页/翻章动作（滚动模式）
            fun scrollPrev() {
                if (isContinuous) {
                    scope.launch {
                        val curIdx = lazyListState.firstVisibleItemIndex
                        val curOff = lazyListState.firstVisibleItemScrollOffset
                        val half = (heightPx * 0.9f).toInt()
                        val newOff = curOff - half
                        if (newOff >= 0) {
                            lazyListState.animateScrollToItem(curIdx, newOff)
                        } else if (curIdx > 0) {
                            lazyListState.animateScrollToItem(curIdx - 1, 0)
                        }
                    }
                } else if (scrollState.value > 0) {
                    scope.launch {
                        scrollState.animateScrollTo(
                            (scrollState.value - heightPx * 0.9f).toInt().coerceAtLeast(0),
                        )
                    }
                } else {
                    viewModel.prevChapter()
                }
            }

            fun scrollNext() {
                if (isContinuous) {
                    scope.launch {
                        val curIdx = lazyListState.firstVisibleItemIndex
                        val curOff = lazyListState.firstVisibleItemScrollOffset
                        val half = (heightPx * 0.9f).toInt()
                        lazyListState.animateScrollToItem(curIdx, (curOff + half).coerceAtLeast(0))
                    }
                } else {
                    val maxValue = scrollState.maxValue
                    if (maxValue != Int.MAX_VALUE && scrollState.value < maxValue) {
                        scope.launch {
                            scrollState.animateScrollTo(
                                (scrollState.value + heightPx * 0.9f).toInt().coerceAtMost(maxValue),
                            )
                        }
                    } else {
                        viewModel.nextChapter()
                    }
                }
            }

            fun onPrev() {
                if (state.settings.pageMode == ReaderSettings.MODE_COVER) coverPrev() else scrollPrev()
            }

            fun onNext() {
                if (state.settings.pageMode == ReaderSettings.MODE_COVER) coverNext() else scrollNext()
            }

            fun handleTap(x: Float) {
                if (state.menuVisible) {
                    viewModel.hideMenu()
                    return
                }
                when {
                    x < widthPx / 3f -> onPrev()
                    x > widthPx * 2f / 3f -> onNext()
                    else -> viewModel.toggleMenu()
                }
            }

            // 音量键事件
            LaunchedEffect(state.settings.pageMode) {
                VolumeKeyBus.events.collect { keyCode ->
                    when (keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> onPrev()
                        KeyEvent.KEYCODE_VOLUME_DOWN -> onNext()
                    }
                }
            }

            // 换章后重置滚动位置（连续模式不重置：由 LazyColumn 自行管理滚动流）
            LaunchedEffect(state.currentChapterId) {
                if (!isContinuous) {
                    scrollState.scrollTo(0)
                    if (!pendingJumpToEnd && pagerState.currentPage != 0) {
                        pagerState.scrollToPage(0)
                    }
                }
            }
            LaunchedEffect(pages.value) {
                if (pendingJumpToEnd && pages.value.isNotEmpty()) {
                    pagerState.scrollToPage(pages.value.lastIndex)
                    pendingJumpToEnd = false
                }
            }

            // 连续滚动核心：滚到窗口边缘时无缝 append/prepend
            LaunchedEffect(isContinuous) {
                if (isContinuous) {
                    snapshotFlow {
                        val info = lazyListState.layoutInfo
                        val first = lazyListState.firstVisibleItemIndex
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: first
                        first to last
                    }.collect { (firstIdx, lastIdx) ->
                        val s = viewModel.uiState.value
                        if (!s.isContinuous) return@collect
                        if (lastIdx >= s.chapterBlocks.lastIndex &&
                            s.hasNextChapter && !s.blockAppendLoading
                        ) {
                            viewModel.appendNextChapter()
                        }
                        if (firstIdx <= 1 && s.hasPrevChapter &&
                            !s.blockPrependLoading && s.chapterBlocks.size > 1
                        ) {
                            viewModel.prependPrevChapter()
                        }
                        val top = s.chapterBlocks.getOrNull(firstIdx)
                        if (top != null && top.id != s.currentChapterId) {
                            viewModel.setCurrentChapter(top.id)
                        }
                    }
                }
            }

            // TOC 跳转 / 上一章下一章按钮：平滑滚动到目标章块
            LaunchedEffect(state.pendingScrollToChapterId) {
                val id = state.pendingScrollToChapterId ?: return@LaunchedEffect
                val idx = state.chapterBlocks.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    lazyListState.animateScrollToItem(idx)
                }
                viewModel.clearPendingScroll()
            }

            // 向上预读时保持原可见位置
            var prependAnchor by remember { mutableStateOf<Pair<String, Int>?>(null) }
            LaunchedEffect(state.blockPrependLoading) {
                if (state.blockPrependLoading) {
                    val idx = lazyListState.firstVisibleItemIndex
                    val key = state.chapterBlocks.getOrNull(idx)?.id
                    prependAnchor = key?.let { it to lazyListState.firstVisibleItemScrollOffset }
                } else if (prependAnchor != null) {
                    val (key, offset) = prependAnchor!!
                    val newIdx = state.chapterBlocks.indexOfFirst { it.id == key }
                    if (newIdx >= 0) lazyListState.scrollToItem(newIdx, offset)
                    prependAnchor = null
                }
            }

            // 阅读模式切换：内容形态不匹配时重开当前章
            LaunchedEffect(state.settings.continuousScroll, state.settings.pageMode) {
                viewModel.reopen()
            }

            // 书内搜索（防抖）
            LaunchedEffect(searchQuery) {
                val q = searchQuery.trim()
                if (q.isEmpty()) {
                    searchResults = emptyList()
                    searching = false
                    return@LaunchedEffect
                }
                searching = true
                delay(250)
                searchResults = viewModel.searchBook(q)
                searching = false
            }

            // ── 内容区 ──
            val showSpinner = state.isLoading &&
                if (state.isContinuous) state.chapterBlocks.isEmpty() else state.rawContent.isEmpty()
            val showError = state.error != null &&
                if (state.isContinuous) state.chapterBlocks.isEmpty() else state.rawContent.isEmpty()
            when {
                showSpinner -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                showError -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = state.error ?: "加载失败", color = theme.text)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("重试")
                        }
                    }
                }

                state.settings.pageMode == ReaderSettings.MODE_COVER && !paginationFailed -> {
                    if (pages.value.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { pageIndex ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = hMarginDp)
                                    .padding(top = 12.dp, bottom = 10.dp)
                                    .pointerInput(state.menuVisible) {
                                        detectTapGestures { offset -> handleTap(offset.x) }
                                    },
                            ) {
                                Text(
                                    text = state.chapterTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = theme.text.copy(alpha = 0.55f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pages.value.getOrElse(pageIndex) { "" },
                                    fontSize = state.settings.fontSize.sp,
                                    lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                                    color = theme.text,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "第 ${state.currentIndex + 1}/${state.totalChapters} 章",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.text.copy(alpha = 0.45f),
                                    )
                                    Text(
                                        text = "${pageIndex + 1}/${pages.value.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = theme.text.copy(alpha = 0.45f),
                                    )
                                }
                            }
                        }
                    }
                }

                state.isContinuous -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = hMarginDp)
                            .pointerInput(state.menuVisible) {
                                detectTapGestures { offset -> handleTap(offset.x) }
                            },
                    ) {
                        items(
                            items = state.chapterBlocks,
                            key = { it.id },
                        ) { block ->
                            ChapterBlockView(
                                block = block,
                                theme = theme,
                                settings = state.settings,
                                highlights = viewModel.highlightsForChapter(block.id),
                                selection = if (pendingSel?.first == block.id) pendingSel!!.second else null,
                                onSelectionChange = { sel -> handleSelection(block.id, sel) },
                                onTextLayout = { layoutCache[block.id] = it },
                            )
                        }
                        item {
                            when {
                                state.blockAppendLoading -> {
                                    Box(
                                        Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
                                }
                                !state.hasNextChapter -> {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "—— 全文完 ——",
                                            color = theme.text.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "下滑继续阅读",
                                            color = theme.text.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = hMarginDp)
                            .pointerInput(state.menuVisible) {
                                detectTapGestures { offset -> handleTap(offset.x) }
                            },
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.chapterTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.text,
                        )
                        if (state.totalChapters > 0) {
                            Text(
                                text = "第 ${state.currentIndex + 1} / ${state.totalChapters} 章",
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.text.copy(alpha = 0.55f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val singleHighlights = viewModel.highlightsForChapter(state.currentChapterId)
                        val singleAnnotated = remember(processed, singleHighlights) {
                            buildAnnotatedString {
                                append(processed)
                                singleHighlights.sortedBy { it.startOffset }.forEach { hl ->
                                    val len = processed.length
                                    val s = hl.startOffset.coerceIn(0, len)
                                    val e = hl.endOffset.coerceIn(s, len)
                                    if (e > s) addStyle(
                                        SpanStyle(background = Color(hl.color ?: DEFAULT_HIGHLIGHT_COLOR).copy(alpha = 0.4f)),
                                        s,
                                        e,
                                    )
                                }
                            }
                        }
                        SelectionText(
                            base = singleAnnotated,
                            selection = if (pendingSel?.first == state.currentChapterId) pendingSel!!.second else null,
                            onSelectionChange = { range -> handleSelection(state.currentChapterId, range) },
                            fontSize = state.settings.fontSize.sp,
                            lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                            color = theme.text,
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = { viewModel.prevChapter() },
                                enabled = state.prevChapterId != null,
                            ) {
                                Text(
                                    "上一章",
                                    color = if (state.prevChapterId != null) theme.text
                                    else theme.text.copy(alpha = 0.35f),
                                )
                            }
                            TextButton(
                                onClick = { viewModel.nextChapter() },
                                enabled = state.nextChapterId != null,
                            ) {
                                Text(
                                    "下一章",
                                    color = if (state.nextChapterId != null) theme.text
                                    else theme.text.copy(alpha = 0.35f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }

            // ── 离线缓存提示条 ──
            if (state.isOffline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⚠ 离线缓存内容，可能不是最新",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // ── 菜单层 ──
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = state.menuVisible,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp),
                    enter = fadeIn(animationSpec = tween(220)) +
                        slideInVertically(initialOffsetY = { -it }, animationSpec = tween(260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(200, easing = FastOutSlowInEasing)),
                ) {
                    ReaderTopBar(
                        bookName = state.bookName,
                        chapterProgress = if (state.totalChapters > 0)
                            "${state.currentIndex + 1}/${state.totalChapters}" else "",
                        onBack = onBack,
                    )
                }

                AnimatedVisibility(
                    visible = state.menuVisible,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp),
                    enter = fadeIn(animationSpec = tween(220)) +
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200, easing = FastOutSlowInEasing)),
                ) {
                    ReaderBottomBar(
                        hasPrev = state.prevChapterId != null,
                        hasNext = state.nextChapterId != null,
                        onToc = { scope.launch { drawerState.open() } },
                        onPrev = { viewModel.prevChapter() },
                        onSettings = { viewModel.showSettings() },
                        onNext = { viewModel.nextChapter() },
                        onAnnotations = { showAnnotations = true },
                        onSearch = {
                            searchQuery = ""
                            searchResults = emptyList()
                            showSearch = true
                        },
                    )
                }
            }

            // ── 选区工具栏（划词后出现）──
            val selInfo = pendingSel
            if (selInfo != null) {
                val (selChId, sel) = selInfo
                val block = state.chapterBlocks.firstOrNull { it.id == selChId }
                val rawText = block?.content ?: if (selChId == state.currentChapterId) processed else ""
                val start = minOf(sel.start, sel.end).coerceIn(0, rawText.length)
                val end = maxOf(sel.start, sel.end).coerceIn(0, rawText.length)
                val selectedText = if (start < end) rawText.substring(start, end) else ""
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 86.dp),
                    enter = fadeIn(animationSpec = tween(180)) +
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(200, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(160)),
                ) {
                    SelectionToolbar(
                        onColor = { color ->
                            viewModel.addHighlight(selChId, start, end, selectedText, color.value.toInt())
                            pendingSel = null
                        },
                        onNote = {
                            val ann = viewModel.addHighlight(selChId, start, end, selectedText, DEFAULT_HIGHLIGHT_COLOR)
                            noteTargetClientId = ann.clientId
                            noteText = ""
                            showNoteDialog = true
                            pendingSel = null
                        },
                        onBookmark = {
                            viewModel.addBookmark(selChId, start, selectedText, null)
                            pendingSel = null
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(selectedText))
                            pendingSel = null
                        },
                        onSearch = {
                            searchQuery = selectedText
                            showSearch = true
                            pendingSel = null
                        },
                        onCancel = { pendingSel = null },
                    )
                }
            }

            // 设置面板
            if (state.settingsVisible) {
                ModalBottomSheet(onDismissRequest = viewModel::hideSettings) {
                    ReaderSettingsSheet(
                        settings = state.settings,
                        onChange = viewModel::updateSettings,
                    )
                }
            }

            // 标注 / 书签面板
            if (showAnnotations) {
                ModalBottomSheet(onDismissRequest = { showAnnotations = false }) {
                    AnnotationsSheet(
                        annotations = state.annotations,
                        toc = state.toc,
                        onJump = { chId -> jumpToChapter(chId); showAnnotations = false },
                        onDelete = viewModel::deleteAnnotation,
                        onEditNote = { clientId ->
                            noteTargetClientId = clientId
                            noteText = state.annotations.firstOrNull { it.clientId == clientId }?.note ?: ""
                            showNoteDialog = true
                        },
                        onExport = { exportAnnotations() },
                        onDismiss = { showAnnotations = false },
                    )
                }
            }

            // 书内搜索面板
            if (showSearch) {
                ModalBottomSheet(onDismissRequest = { showSearch = false }) {
                    SearchSheet(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        results = searchResults,
                        searching = searching,
                        onResultClick = { m ->
                            jumpToChapter(m.chapterId)
                            showSearch = false
                        },
                        onDismiss = { showSearch = false },
                    )
                }
            }

            // 笔记编辑弹窗
            if (showNoteDialog && noteTargetClientId != null) {
                AlertDialog(
                    onDismissRequest = { showNoteDialog = false },
                    title = { Text("编辑笔记") },
                    text = {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("笔记内容") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateAnnotationNote(noteTargetClientId!!, noteText.trim())
                            showNoteDialog = false
                        }) { Text("保存") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNoteDialog = false }) { Text("取消") }
                    },
                )
            }
        }
    }
}

/**
 * 连续滚动流中的单个章节块：标题 + 正文（可选中划词）+ 章节分隔留白。
 * 高亮以背景色渲染在正文上；选区由外层 SelectionContainer 管理。
 */
@Composable
private fun ChapterBlockView(
    block: ChapterBlock,
    theme: ReaderTheme,
    settings: ReaderSettings,
    highlights: List<AnnotationEntity>,
    selection: TextRange?,
    onSelectionChange: (TextRange?) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    val annotated = remember(block.content, highlights) {
        buildAnnotatedString {
            append(block.content)
            highlights.sortedBy { it.startOffset }.forEach { hl ->
                val len = block.content.length
                val s = hl.startOffset.coerceIn(0, len)
                val e = hl.endOffset.coerceIn(s, len)
                if (e > s) {
                    addStyle(
                        SpanStyle(background = Color(hl.color ?: DEFAULT_HIGHLIGHT_COLOR).copy(alpha = 0.4f)),
                        s,
                        e,
                    )
                }
            }
        }
    }
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
        SelectionText(
            base = annotated,
            selection = selection,
            onSelectionChange = onSelectionChange,
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
            color = theme.text,
            onTextLayout = onTextLayout,
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

/**
 * 可选中正文：拖拽手势采集章内字符偏移（TextRange），渲染实时选区高亮。
 * 不依赖 Compose 内部 SelectionContainer 的受控重载（其为 internal，外部不可用）。
 */
@Composable
private fun SelectionText(
    base: AnnotatedString,
    selection: TextRange?,
    onSelectionChange: (TextRange?) -> Unit,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var dragStart by remember { mutableStateOf(-1) }
    val len = base.length
    val display = remember(base, selection) {
        buildAnnotatedString {
            append(base)
            if (selection != null) {
                val s = selection.start.coerceIn(0, len)
                val e = selection.end.coerceIn(s, len)
                if (e > s) {
                    addStyle(SpanStyle(background = Color(DEFAULT_HIGHLIGHT_COLOR).copy(alpha = 0.4f)), s, e)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val l = layoutResult ?: return@detectDragGestures
                        val o = l.getOffsetForPosition(offset)
                        dragStart = o
                        onSelectionChange(TextRange(o, o))
                    },
                    onDrag = { change, _ ->
                        val l = layoutResult ?: return@detectDragGestures
                        val o = l.getOffsetForPosition(change.position)
                        onSelectionChange(TextRange(dragStart, o))
                    },
                )
            },
    ) {
        Text(
            text = display,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = color,
            onTextLayout = {
                layoutResult = it
                onTextLayout(it)
            },
        )
    }
}

/** 划词选区工具栏：多色划线 / 笔记 / 书签 / 复制 / 搜索 / 取消 */
@Composable
private fun SelectionToolbar(
    onColor: (Color) -> Unit,
    onNote: () -> Unit,
    onBookmark: () -> Unit,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = OverlayBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HIGHLIGHT_COLORS.forEach { c ->
                IconButton(onClick = { onColor(c) }) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "划线",
                        tint = c,
                    )
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
            IconButton(onClick = onNote) {
                Icon(Icons.Filled.Edit, contentDescription = "笔记", tint = OverlayText)
            }
            IconButton(onClick = onBookmark) {
                Icon(Icons.Filled.Bookmark, contentDescription = "书签", tint = OverlayText)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "复制", tint = OverlayText)
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = OverlayText)
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "取消", tint = OverlayTextDim)
            }
        }
    }
}

/** 标注 / 书签列表面板 */
@Composable
private fun AnnotationsSheet(
    annotations: List<AnnotationEntity>,
    toc: List<ChapterDto>,
    onJump: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditNote: (String) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val chapterTitle = { id: String -> toc.firstOrNull { it.id == id }?.name ?: id }
    var tab by remember { mutableStateOf(0) }
    val highlights = annotations.filter { it.type == ANNOTATION_TYPE_HIGHLIGHT }
    val bookmarks = annotations.filter { it.type == ANNOTATION_TYPE_BOOKMARK }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("标注与书签", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onExport) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("导出")
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭") }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = { tab = 0 }) { Text("划线 (${highlights.size})", color = if (tab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { tab = 1 }) { Text("书签 (${bookmarks.size})", color = if (tab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val list = if (tab == 0) highlights else bookmarks
        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("暂无${if (tab == 0) "划线" else "书签"}，长按正文即可添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(list, key = { it.clientId }) { a ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            chapterTitle(a.chapterId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (a.type == ANNOTATION_TYPE_HIGHLIGHT) {
                            Text(
                                a.quotedText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                a.note ?: a.quotedText ?: "（书签）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!a.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("笔记：${a.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onJump(a.chapterId) }) { Text("跳转") }
                            TextButton(onClick = { onEditNote(a.clientId) }) { Text("笔记") }
                            TextButton(onClick = { onDelete(a.clientId) }) { Text("删除") }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 书内搜索面板 */
@Composable
private fun SearchSheet(
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

// 菜单遮罩底色（所有阅读主题下都可读）
private val OverlayBg = Color(0xCC14181D)
private val OverlayText = Color.White
private val OverlayTextDim = Color.White.copy(alpha = 0.35f)

// 复用 ReaderMenuBar 的轻量圆角与分割线
private val MenuCardRadius = 16.dp

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}
