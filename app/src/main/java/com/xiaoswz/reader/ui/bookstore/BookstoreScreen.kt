package com.xiaoswz.reader.ui.bookstore

import com.xiaoswz.reader.ui.components.AppTopBar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.data.model.BookDto
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.bookshelf.BookEntity
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.data.model.resolveCoverUrl
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.update.UpdateManager
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.SectionHeader
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 首页（0.8.2 由「书城」拆出）：保持整洁——只放欢迎 Hero + 热度轮播 + 浏览书库入口。
 * 排行榜 / 月票榜 / 最新上架 / 分区浏览 全部移到独立的「书库」页（Routes.BOOKLIBRARY），
 * 底部导航不新增书库 tab，书库仅由本页入口进入。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onBookClick: (String) -> Unit,
    onBrowseLibrary: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val featured = state.books.take(8)

    // 「继续阅读」：纯本地书架进度，零网络依赖（粉丝向社区的核心留存入口）
    val appCtx = LocalContext.current.applicationContext
    val bookshelfRepo = remember { BookshelfRepository(appCtx) }
    val shelfBooks by bookshelfRepo.observeAll().collectAsState(initial = emptyList())
    val continueBooks = remember(shelfBooks) {
        shelfBooks
            .filter { it.lastChapterId != null }
            .sortedByDescending { it.lastReadAt }
            .take(8)
    }

    // ── 局域网自动更新（启动即检查一次）──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { ReaderSettingsRepository(context.applicationContext) }
    var updateServerUrl by remember { mutableStateOf(BuildConfig.DEFAULT_UPDATE_SERVER) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogAutoCheck by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateServerUrl = settingsRepo.settingsFlow.first().updateServerUrl
        UpdateManager(context.applicationContext).check(updateServerUrl)
            .onSuccess { info ->
                if (info != null) {
                    updateDialogAutoCheck = true
                    showUpdateDialog = true
                }
            }
    }

    if (showUpdateDialog) {
        UpdateDialog(
            serverUrl = updateServerUrl,
            autoCheck = updateDialogAutoCheck,
            onServerUrlChange = { newUrl ->
                updateServerUrl = newUrl
                scope.launch { settingsRepo.update { it.copy(updateServerUrl = newUrl) } }
            },
            onDismiss = { showUpdateDialog = false },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = null,
                actions = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "检查更新",
                        tint = GlassTokens.Label,
                        modifier = Modifier
                            .clickable(onClick = {
                                updateDialogAutoCheck = false
                                showUpdateDialog = true
                            })
                            .padding(6.dp)
                            .size(26.dp),
                    )
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        val pullState = rememberPullRefreshState(
            refreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
        )
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullState),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AnibuzzHeroCard(onBrowseLibrary = onBrowseLibrary)
                }
                item {
                    SectionHeader(title = "本周热度")
                }
                item {
                    if (featured.isNotEmpty()) {
                        FeaturedCarousel(books = featured, onBookClick = onBookClick)
                    } else if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = WhaleColors.WhaleBlue,
                            )
                        }
                    }
                }
                item {
                    if (continueBooks.isNotEmpty()) {
                        SectionHeader(title = "继续阅读")
                    }
                }
                item {
                    if (continueBooks.isNotEmpty()) {
                        ContinueReadingRow(books = continueBooks, onBookClick = onBookClick)
                    }
                }
                item {
                    BrowseLibraryCard(onBrowseLibrary = onBrowseLibrary)
                }
            }
            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = WhaleColors.WhaleBlue,
            )
        }
    }
}

/** 「浏览书库」入口卡片：从首页一键进入书库（排行榜 / 月票榜 / 分区浏览 / 最新上架） */
@Composable
private fun BrowseLibraryCard(onBrowseLibrary: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onBrowseLibrary),
        radius = GlassTokens.RadiusXL,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "浏览书库",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "热榜 · 月票榜 · 分区浏览 · 最新上架",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassTokens.GradientButton),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "进入书库",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  首页「继续阅读」（纯本地书架进度，零网络）
// ══════════════════════════════════════════════════════════

/** 继续阅读横滑行：展示本地书架里「有阅读进度」的书，按最近阅读时间排序 */
@Composable
private fun ContinueReadingRow(
    books: List<BookEntity>,
    onBookClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lazyItems(
            items = books,
            key = { book: BookEntity -> book.slug },
        ) { book: BookEntity ->
            ContinueReadingCard(book = book, onClick = { onBookClick(book.slug) })
        }
    }
}

/** 单张继续阅读卡片：封面 + 底部「继续」标签 + 书名 + 上次阅读章节 */
@Composable
private fun ContinueReadingCard(
    book: BookEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassTokens.GlassFillStrong),
        ) {
            AsyncImage(
                model = resolveCoverUrl(book.coverUrl),
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0D1B2A).copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WhaleColors.WhaleBlue.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "继续",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = GlassTokens.Label,
        )
        if (!book.lastChapterTitle.isNullOrBlank()) {
            Text(
                text = book.lastChapterTitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = GlassTokens.SecondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
//  首页共享装饰组件（Hero / 轮播）
// ══════════════════════════════════════════════════════════

/** iOS 玻璃风 Hero 卡片：磨砂玻璃容器 + 品牌文字 + CTA 按钮（点击进入书库） */
@Composable
private fun AnibuzzHeroCard(onBrowseLibrary: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        radius = GlassTokens.RadiusXL,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "冲浪阅读",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTokens.SystemBlue,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "欢迎回来",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.Label,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "发现属于你的下一本好书",
                style = MaterialTheme.typography.bodyLarge,
                color = GlassTokens.SecondaryLabel,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                    .background(GlassTokens.GradientButton)
                    .clickable { onBrowseLibrary() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "开始探索",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }
    }
}

/** 封面轮播 Banner：渐变遮罩 + 标题 + 状态标签 + 自动轮播（首页「本周热度」） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturedCarousel(
    books: List<BookDto>,
    onBookClick: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { books.size })

    LaunchedEffect(pagerState, books.size) {
        if (books.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % books.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp)),
            pageSpacing = 12.dp,
        ) { page ->
            val book = books[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { book.slug?.let(onBookClick) },
            ) {
                AsyncImage(
                    model = resolveCoverUrl(book.coverImage),
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0D1B2A).copy(alpha = 0.85f),
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    if (book.statusText.isNotBlank()) {
                        com.xiaoswz.reader.ui.components.StatusPill(text = book.statusText)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = book.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = book.displayAuthor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(books.size) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 18.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (selected) WhaleColors.WhaleBlue
                            else WhaleColors.TextDisabled.copy(alpha = 0.4f),
                        ),
                )
            }
        }
    }
}
