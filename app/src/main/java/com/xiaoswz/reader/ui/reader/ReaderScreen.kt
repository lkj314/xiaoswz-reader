package com.xiaoswz.reader.ui.reader

import android.app.Activity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.xiaoswz.reader.ui.reader.components.ReaderBottomBar
import com.xiaoswz.reader.ui.reader.components.ReaderTopBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.CrashLogger
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.ui.theme.ReaderThemes
import com.xiaoswz.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** 左右边距档位对应的 dp */
private val MarginDpOptions = listOf(12.dp, 20.dp, 28.dp)

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
    // 协程级异常兜底：捕获阅读器内所有未处理异常，写入崩溃日志，避免闪退
    val scope = rememberCoroutineScope {
        CoroutineExceptionHandler { _, t -> CrashLogger.report(context, t) }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

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
            // 覆盖模式：页面上部标题区 + 底部进度区预留
            val reservedHeightPx = with(density) { 64.dp.toPx() }

            val scrollState = rememberScrollState()
            val lazyListState = rememberLazyListState()
            val textMeasurer = rememberTextMeasurer()
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

            // 覆盖模式分页（在协程中计算，避免组合阶段同步测量整章卡顿/崩溃）
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
                    // 分页异常兜底：降级为滚动模式，保证可读、不闪退
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
                    // 连续流里向上整页平滑滚动（不换章，无缝）
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
                    // 连续流里向下整页平滑滚动（不换章，无缝）
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
            // 上一章切换完成后跳到其末页
            LaunchedEffect(pages.value) {
                if (pendingJumpToEnd && pages.value.isNotEmpty()) {
                    pagerState.scrollToPage(pages.value.lastIndex)
                    pendingJumpToEnd = false
                }
            }

            // ── 连续滚动（无缝阅读）核心：滚动到窗口边缘时无缝 append/prepend ──
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
                        // 滚到末尾 → 把下一章接进同一条流（缓存命中即瞬时，无感）
                        if (lastIdx >= s.chapterBlocks.lastIndex &&
                            s.hasNextChapter && !s.blockAppendLoading
                        ) {
                            viewModel.appendNextChapter()
                        }
                        // 滚到顶部 → 把上一章接进流（仅多章时才预读，避免首章抖动）
                        if (firstIdx <= 1 && s.hasPrevChapter &&
                            !s.blockPrependLoading && s.chapterBlocks.size > 1
                        ) {
                            viewModel.prependPrevChapter()
                        }
                        // 上报顶部可见章（进度保存 + 预读推进）
                        val top = s.chapterBlocks.getOrNull(firstIdx)
                        if (top != null && top.id != s.currentChapterId) {
                            viewModel.setCurrentChapter(top.id)
                        }
                    }
                }
            }

            // ── TOC 跳转 / 上一章下一章按钮：平滑滚动到目标章块（不换屏）──
            LaunchedEffect(state.pendingScrollToChapterId) {
                val id = state.pendingScrollToChapterId ?: return@LaunchedEffect
                val idx = state.chapterBlocks.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    lazyListState.animateScrollToItem(idx)
                }
                viewModel.clearPendingScroll()
            }

            // ── 向上预读时保持原可见位置（prepend 后内容整体上移，需还原）──
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

            // ── 阅读模式切换（翻页↔滚动 / 连续开关）：内容形态不匹配时重开当前章 ──
            LaunchedEffect(state.settings.continuousScroll, state.settings.pageMode) {
                viewModel.reopen()
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
                    // 连续滚动（无缝阅读）：所有章节拼成同一条滚动流，滚到底自然接续下一章
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
                            ChapterBlockView(block = block, theme = theme, settings = state.settings)
                        }
                        // 末尾：加载中 / 全文完 / 下滑提示
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
                    // 滚动模式（单章）：保留原「一章一屏」行为，供不需要连续流的场景使用
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
                        Text(
                            text = processed,
                            fontSize = state.settings.fontSize.sp,
                            lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                            color = theme.text,
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        // 章末引导
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

            // ── 离线缓存提示条（内容来自磁盘文件、断网可读）──
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

            // ── 菜单层（圆角浮动卡片 + 图标按钮 + 进出动画）──
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = state.menuVisible,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp),
                    enter = fadeIn(animationSpec = tween(220)) +
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                        ),
                    exit = fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        ),
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
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                        ),
                    exit = fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        ),
                ) {
                    ReaderBottomBar(
                        hasPrev = state.prevChapterId != null,
                        hasNext = state.nextChapterId != null,
                        onToc = { scope.launch { drawerState.open() } },
                        onPrev = { viewModel.prevChapter() },
                        onSettings = { viewModel.showSettings() },
                        onNext = { viewModel.nextChapter() },
                    )
                }
            }

            // ── 设置面板 ──
            if (state.settingsVisible) {
                ModalBottomSheet(onDismissRequest = viewModel::hideSettings) {
                    ReaderSettingsSheet(
                        settings = state.settings,
                        onChange = viewModel::updateSettings,
                    )
                }
            }
        }
    }
}

/**
 * 连续滚动流中的单个章节块：标题 + 正文 + 章节分隔留白。
 * 多章在一条 LazyColumn 中首尾相连，滚到底自然接续下一章（无缝衔接）。
 */
@Composable
private fun ChapterBlockView(
    block: ChapterBlock,
    theme: ReaderTheme,
    settings: ReaderSettings,
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
        Text(
            text = block.content,
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
            color = theme.text,
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}
