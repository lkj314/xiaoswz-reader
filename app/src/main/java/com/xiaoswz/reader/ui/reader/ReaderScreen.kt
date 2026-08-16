package com.xiaoswz.reader.ui.reader

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.KeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.selection.SelectionContainer
import android.view.WindowManager
import java.util.Locale
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.CrashLogger
import com.xiaoswz.reader.data.model.ChapterDto
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.data.community.CommunityRepository
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.annotation.AnnotationRepository
import com.xiaoswz.reader.data.plugin.PluginManifest
import com.xiaoswz.reader.data.plugin.PluginRepository
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

    // ── 书内搜索 UI 状态 ──
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchMatch>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    // 休息提醒（护眼模块C）：连续阅读计时起点 + 是否弹出提醒
    var showRestReminder by remember { mutableStateOf(false) }
    val restStartMs = remember { mutableStateOf(System.currentTimeMillis()) }
    // 分享到书友圈（模块D）
    var showShare by remember { mutableStateOf(false) }

    // ── 听书 TTS 状态 ──
    val ttsEngine = remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsSpeaking by remember { mutableStateOf(false) }
    var ttsStarted by remember { mutableStateOf(false) } // 是否已开始过（用于暂停后续读 + 锚点显示）
    val ttsIndex = remember { mutableStateOf(0) }
    val ttsSentences = remember { mutableStateOf<List<String>>(emptyList()) }
    val ttsSentenceRanges = remember { mutableStateOf<List<SentenceRange>>(emptyList()) } // 每句在原文中的 [start,end)，供锚点高亮
    val ttsChapterId = remember { mutableStateOf<String?>(null) } // TTS 当前断句对应的是哪一章
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(bookSlug, chapterId) {
        viewModel.load(bookSlug, chapterId)
    }

    // ── 创意工坊 v1：划词标注（opt-in 只读 BasicTextField，选区偏移相对预处理正文）──
    // 本书全部标注（本地为权威），用于 decorator 锚点渲染。
    val annotations = remember { mutableStateListOf<AnnotationEntity>() }
    LaunchedEffect(bookSlug) {
        runCatching { annotations.clear(); annotations.addAll(AnnotationRepository.loadLocal(context, bookSlug)) }
            .onFailure { CrashLogger.report(context, it) }
    }
    // 当前生效的「选区动作」类插件（官方高亮/书签 + 用户安装），驱动选区菜单动态追加项。
    val annoPlugins by PluginRepository.annotationPlugins(context)
        .collectAsState(initial = emptyList())
    // 划词标注模式开关 + 只读文本字段（承载选区状态，offset 相对预处理正文）
    var annoActive by remember { mutableStateOf(false) }
    val annoTextField = remember { mutableStateOf(TextFieldValue(text = "", selection = TextRange.Zero)) }

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

    // 休息提醒计时（护眼模块C）：开启后每 30s 检查连续阅读时长，达阈值弹提醒（不阻断阅读）
    LaunchedEffect(state.settings.restReminderEnabled, state.settings.restReminderMinutes) {
        if (!state.settings.restReminderEnabled) return@LaunchedEffect
        val threshold = state.settings.restReminderMinutes * 60_000L
        while (true) {
            delay(30_000L)
            if (!showRestReminder &&
                System.currentTimeMillis() - restStartMs.value >= threshold
            ) {
                showRestReminder = true
            }
        }
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

            // 划词标注：进入/换章时同步只读文本字段（offset 相对预处理正文）
            LaunchedEffect(processed, annoActive) {
                if (annoActive) annoTextField.value = TextFieldValue(text = processed, selection = TextRange.Zero)
            }
            // 离开「单章滚动」模式时关闭划词标注（覆盖/连续模式不支持选区偏移映射）
            LaunchedEffect(state.settings.pageMode, isContinuous) {
                if (!(state.settings.pageMode == ReaderSettings.MODE_SCROLL && !isContinuous)) annoActive = false
            }

            /** 选区 → 写入一条标注（落本地 + 推云端 + 即时刷新 decorator），并收起选区 */
            fun annotateFromSelection(plugin: PluginManifest, start: Int, end: Int, chapterId: String) {
                val cap = plugin.capabilities.annotation ?: return
                val s = start.coerceAtLeast(0)
                val e = end.coerceAtMost(processed.length)
                if (s >= e) return
                val quoted = processed.substring(s, e)
                val entity = AnnotationRepository.createAnnotation(
                    bookId = bookSlug,
                    chapterId = chapterId,
                    start = s,
                    end = e,
                    quoted = quoted,
                    color = cap.defaultColor ?: -14336,
                    type = cap.annotationType,
                    note = null,
                )
                scope.launch {
                    runCatching {
                        val list = AnnotationRepository.loadLocal(context, bookSlug).toMutableList()
                        list.add(entity)
                        AnnotationRepository.persist(context, bookSlug, list)
                        AnnotationRepository.pushOne(context, entity)
                    }.onFailure { CrashLogger.report(context, it) }
                }
                if (annotations.none { it.clientId == entity.clientId }) annotations.add(entity)
                annoTextField.value = annoTextField.value.copy(selection = TextRange.Zero)
                android.widget.Toast.makeText(context, "已添加「${cap.label}」", android.widget.Toast.LENGTH_SHORT).show()
            }

            // 跳转到某章（搜索结果命中）
            fun jumpToChapter(chapterId: String) {
                viewModel.requestScrollToChapter(chapterId)
            }

            // ── 听书 TTS：逐句朗读当前章，读完自动续下一章 ──
            // 断句并保留每句在原文中的区间，供锚点高亮精确对齐
            fun splitToSentencesWithRanges(text: String): List<SentenceRange> {
                if (text.isEmpty()) return emptyList()
                val regex = Regex("(?<=[。！？!?；;\\n])")
                val ranges = mutableListOf<SentenceRange>()
                var cursor = 0
                for (part in regex.split(text)) {
                    val trimmed = part.trim()
                    if (trimmed.isBlank()) { cursor += part.length; continue }
                    val start = cursor + part.indexOf(trimmed)
                    val end = start + trimmed.length
                    ranges.add(SentenceRange(trimmed, start, end))
                    cursor += part.length
                }
                return ranges
            }

            fun speakCurrentSentence() {
                val engine = ttsEngine.value ?: return
                val list = ttsSentences.value
                val idx = ttsIndex.value
                if (idx >= list.size) {
                    // 本章读完：自动续读下一章（currentChapterId 变化会触发下方 LaunchedEffect 重新朗读）
                    if (viewModel.uiState.value.hasNextChapter) {
                        ttsIndex.value = 0
                        viewModel.nextChapter()
                    } else {
                        ttsSpeaking = false
                        engine.stop()
                    }
                    return
                }
                engine.setSpeechRate(state.settings.ttsRate)
                engine.speak(list[idx], TextToSpeech.QUEUE_FLUSH, null, "utt_$idx")
            }

            fun advanceTts() {
                ttsIndex.value += 1
                speakCurrentSentence()
            }

            fun startTts() {
                val text = viewModel.currentChapterProcessedText()
                if (text.isBlank()) { ttsSpeaking = false; return }
                ttsStarted = true
                // 不在此清零 ttsIndex：同章暂停后再次播放由下方 LaunchedEffect 从原位置续读；
                // 若章节已变（ttsChapterId != 当前章），LaunchedEffect 会重新断句并从章首开始。
                ttsSpeaking = true
            }

            fun stopTts() {
                ttsEngine.value?.stop()
                ttsSpeaking = false // 仅暂停：保留 ttsIndex / ttsStarted / ttsSentenceRanges，下次续读
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

            // 听书 TTS 引擎初始化 / 退出销毁
            DisposableEffect(Unit) {
                val engine = TextToSpeech(context.applicationContext, null)
                engine.language = Locale.SIMPLIFIED_CHINESE
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { advanceTts() }
                    }
                    override fun onError(utteranceId: String?) {}
                })
                ttsEngine.value = engine
                onDispose {
                    engine.stop()
                    engine.shutdown()
                    ttsEngine.value = null
                }
            }

            // 听书：开始 / 切章时朗读当前章（引擎就绪才真正发声）
            // 关键：仅当「章节变化」才重新断句并归零；同章内 ttsSpeaking 由 false→true（暂停后续读）
            // 时沿用已有 ttsIndex，从暂停处继续，而不是从头。
            LaunchedEffect(ttsSpeaking, state.currentChapterId, ttsEngine.value != null) {
                if (!ttsSpeaking) return@LaunchedEffect
                val text = viewModel.currentChapterProcessedText()
                if (text.isBlank()) return@LaunchedEffect
                if (ttsChapterId.value != state.currentChapterId) {
                    // 章节变了（首次 / 手动切章 / 自动续章）：重新断句，从章首开始
                    val ranges = splitToSentencesWithRanges(text)
                    ttsSentenceRanges.value = ranges
                    ttsSentences.value = ranges.map { it.text }
                    ttsIndex.value = 0
                    ttsChapterId.value = state.currentChapterId
                }
                if (ttsEngine.value != null) speakCurrentSentence()
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
                                SelectionContainer {
                                    Text(
                                        text = pages.value.getOrElse(pageIndex) { "" },
                                        fontSize = state.settings.fontSize.sp,
                                        lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                                        color = theme.text,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
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
                            val isReadingChapter = ttsStarted && block.id == state.currentChapterId
                            val readingRange = if (isReadingChapter)
                                ttsSentenceRanges.value.getOrNull(ttsIndex.value)
                                    ?.let { it.start until it.end }
                            else null
                            ChapterBlockView(
                                block = block,
                                theme = theme,
                                settings = state.settings,
                                isReadingChapter = isReadingChapter,
                                readingRange = readingRange,
                                annotations = annotations.filter { it.chapterId == block.id },
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
                            .pointerInput(annoActive to state.menuVisible) {
                                if (!annoActive) detectTapGestures { offset -> handleTap(offset.x) }
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
                        val isReadingChapter = ttsStarted
                        val readingRange = if (isReadingChapter)
                            ttsSentenceRanges.value.getOrNull(ttsIndex.value)
                                ?.let { it.start until it.end }
                        else null
                        val annoAnnotations = annotations.filter { it.chapterId == state.currentChapterId }
                        if (annoActive) {
                            val annoTransform = remember(processed, annoAnnotations, readingRange, theme) {
                                VisualTransformation { text ->
                                    TransformedText(
                                        buildAnnotatedContent(
                                            content = text.text,
                                            annotations = annoAnnotations,
                                            readingRange = if (isReadingChapter) readingRange else null,
                                            theme = theme,
                                        ),
                                        OffsetMapping.Identity,
                                    )
                                }
                            }
                            BasicTextField(
                                value = annoTextField.value,
                                onValueChange = { annoTextField.value = it },
                                readOnly = true,
                                textStyle = TextStyle(
                                    color = theme.text,
                                    fontSize = state.settings.fontSize.sp,
                                    lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                                ),
                                visualTransformation = annoTransform,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = buildAnnotatedContent(
                                        content = processed,
                                        annotations = annoAnnotations,
                                        readingRange = if (isReadingChapter) readingRange else null,
                                        theme = theme,
                                    ),
                                    fontSize = state.settings.fontSize.sp,
                                    lineHeight = (state.settings.fontSize * state.settings.lineSpacing).sp,
                                    color = theme.text,
                                )
                            }
                        }
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

            // ── 护眼蓝光过滤层（叠加暖色滤镜，不拦截触摸/滚动）──
            if (state.settings.blueLightFilter) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFB43C).copy(alpha = 0.12f)),
                )
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
                        onTtsToggle = { if (ttsSpeaking) stopTts() else startTts() },
                        ttsActive = ttsSpeaking,
                        onShare = { showShare = true },
                        onSearch = {
                            searchQuery = ""
                            searchResults = emptyList()
                            showSearch = true
                        },
                        annoActive = annoActive,
                        annoAvailable = state.settings.pageMode == ReaderSettings.MODE_SCROLL && !isContinuous,
                        onAnnoToggle = {
                            val next = !annoActive
                            annoActive = next
                            if (next) annoTextField.value = TextFieldValue(text = processed, selection = TextRange.Zero)
                            else annoTextField.value = annoTextField.value.copy(selection = TextRange.Zero)
                        },
                    )
                }
            }

            // 创意工坊 v1：划词标注选区浮层（复制 + 动态插件动作）
            AnnoSelectionBar(
                visible = annoActive &&
                    annoTextField.value.selection.start < annoTextField.value.selection.end,
                plugins = annoPlugins,
                onCopy = {
                    val sel = annoTextField.value.selection
                    if (sel.start < sel.end) {
                        val text = annoTextField.value.text.substring(sel.start, sel.end)
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("标注", text))
                        android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onAnnotate = { plugin ->
                    val sel = annoTextField.value.selection
                    annotateFromSelection(plugin, sel.start, sel.end, state.currentChapterId)
                },
                onClose = { annoTextField.value = annoTextField.value.copy(selection = TextRange.Zero) },
            )

            // 设置面板
            if (state.settingsVisible) {
                ModalBottomSheet(onDismissRequest = viewModel::hideSettings) {
                    ReaderSettingsSheet(
                        settings = state.settings,
                        onChange = viewModel::updateSettings,
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

            // 分享到书友圈（模块D）
            if (showShare) {
                val shareInitial = buildString {
                    append("《${state.bookName}》\n")
                    append(viewModel.currentChapterProcessedText().take(600))
                }
                ModalBottomSheet(onDismissRequest = { showShare = false }) {
                    ReaderShareSheet(initialText = shareInitial, onDismiss = { showShare = false })
                }
            }

            // 休息提醒弹窗（护眼模块C）
            if (showRestReminder) {
                AlertDialog(
                    onDismissRequest = {
                        showRestReminder = false
                        restStartMs.value = System.currentTimeMillis()
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showRestReminder = false
                            restStartMs.value = System.currentTimeMillis()
                        }) { Text("好的，休息一下") }
                    },
                    title = { Text("休息一下吧") },
                    text = {
                        Text(
                            "你已经连续阅读约 ${state.settings.restReminderMinutes} 分钟了，" +
                                "起来活动活动、看看远处，保护眼睛~",
                        )
                    },
                )
            }

        }
    }
}

/**
 * 连续滚动流中的单个章节块：标题 + 正文 + 章节分隔留白。
 * 正文为纯文本渲染；划线/书签等增强能力未来以「创意工坊插件」形式提供，不进入核心。
 */
@Composable
private fun ChapterBlockView(
    block: ChapterBlock,
    theme: ReaderTheme,
    settings: ReaderSettings,
    isReadingChapter: Boolean = false,
    readingRange: IntRange? = null,
    annotations: List<AnnotationEntity> = emptyList(),
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
        SelectionContainer {
            Text(
                text = buildAnnotatedContent(
                    content = block.content,
                    annotations = annotations,
                    readingRange = if (isReadingChapter) readingRange else null,
                    theme = theme,
                ),
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                color = theme.text,
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
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

/**
 * 分享当前书摘到书友圈（模块D）：预填本章标题+正文，可编辑后发布。
 * 复用 CommunityRepository.createPost（需登录；未登录由后端返回 login_required 并映射提示）。
 */
@Composable
private fun ReaderShareSheet(
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
        Text("分享到书友圈", style = MaterialTheme.typography.titleLarge)
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
                            android.widget.Toast.makeText(context, "已分享到书友圈", android.widget.Toast.LENGTH_SHORT).show()
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
            Text(if (posting) "发布中…" else "发布到书友圈")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** 一句在原文中的区间，用于听书锚点高亮与 TTS 索引同步 */
private data class SentenceRange(val text: String, val start: Int, val end: Int)

/**
 * 将正文 + 既有标注 + TTS 朗读区间合并为带背景样式的 AnnotatedString。
 * 标注锚点（decorator 槽）与听书高亮同源渲染，互不干扰；标注偏移相对预处理正文。
 */
private fun buildAnnotatedContent(
    content: String,
    annotations: List<AnnotationEntity>,
    readingRange: IntRange?,
    theme: ReaderTheme,
): AnnotatedString = buildAnnotatedString {
    append(content)
    for (a in annotations) {
        val s = a.startOffset.coerceAtLeast(0)
        val e = a.endOffset.coerceAtMost(content.length)
        if (e > s) {
            val c = a.color?.let { Color(it) } ?: Color(-14336)
            addStyle(SpanStyle(background = c.copy(alpha = 0.22f)), s, e)
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
private fun AnnoSelectionBar(
    visible: Boolean,
    plugins: List<PluginManifest>,
    onCopy: () -> Unit,
    onAnnotate: (PluginManifest) -> Unit,
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
            for (plugin in plugins) {
                val cap = plugin.capabilities.annotation ?: continue
                TextButton(onClick = { onAnnotate(plugin) }) { Text(cap.label) }
            }
            TextButton(onClick = onClose) { Text("完成") }
        }
    }
}
