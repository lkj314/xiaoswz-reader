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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.data.model.resolveCoverUrl
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.update.UpdateManager
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.api.LeaderboardEntry
import com.xiaoswz.reader.ui.components.BookCoverCard
import com.xiaoswz.reader.ui.components.BookCoverSkeleton
import com.xiaoswz.reader.ui.components.EmptyState
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.SectionHeader
import com.xiaoswz.reader.ui.components.StatusPill
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val STATUS_ALL = "all"
private const val STATUS_ONGOING = "ONGOING"
private const val STATUS_COMPLETED = "COMPLETED"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BookstoreScreen(
    onBookClick: (String) -> Unit,
    viewModel: BookstoreViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // ── 局域网自动更新 ──
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

    // 状态筛选（纯前端，按 status 字段过滤已加载列表）
    var statusFilter by remember { mutableStateOf(STATUS_ALL) }

    // 滚动接近底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val statusFiltered = remember(state.books, statusFilter) {
        state.books.filter { b ->
            statusFilter == STATUS_ALL || b.status == statusFilter
        }
    }
    val featured = statusFiltered.take(5)
    val popular = statusFiltered.drop(5).take(8)
    val gridBooks = statusFiltered.drop(13)

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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ══════════════════════════════════════
                //  HERO 卡片：ANIBUZZ Welcome 风格
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    AnibuzzHeroCard()
                }

                // ══════════════════════════════════════
                //  搜索栏 + 筛选标签
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // 玻璃态搜索框（iOS 胶囊式）
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            placeholder = {
                                Text(
                                    "搜索书名 / 简介",
                                    color = GlassTokens.SecondaryLabel,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = GlassTokens.SecondaryLabel,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.refresh() }),
                            shape = RoundedCornerShape(GlassTokens.RadiusPill),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = GlassTokens.GlassFillStrong,
                                unfocusedContainerColor = GlassTokens.GlassFillStrong,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = GlassTokens.SystemBlue,
                                focusedTextColor = GlassTokens.Label,
                                unfocusedTextColor = GlassTokens.Label,
                            ),
                        )
                        // 筛选行
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            StatusFilterChip("全部", statusFilter == STATUS_ALL) {
                                statusFilter = STATUS_ALL
                            }
                            StatusFilterChip("连载中", statusFilter == STATUS_ONGOING) {
                                statusFilter = STATUS_ONGOING
                            }
                            StatusFilterChip("已完结", statusFilter == STATUS_COMPLETED) {
                                statusFilter = STATUS_COMPLETED
                            }
                            Spacer(Modifier.weight(1f))
                            FilterChip(
                                selected = state.sort == BookstoreUiState.SORT_LATEST,
                                onClick = { viewModel.onSortChange(BookstoreUiState.SORT_LATEST) },
                                label = { Text("最新") },
                            )
                            FilterChip(
                                selected = state.sort == BookstoreUiState.SORT_POPULAR,
                                onClick = { viewModel.onSortChange(BookstoreUiState.SORT_POPULAR) },
                                label = { Text("热门") },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ══════════════════════════════════════
                //  排行榜（P1：冲浪阅读自己的热榜/月票榜，绝不汇总主站）
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    SectionHeader(title = "🔥 热榜")
                }
                item(span = { GridItemSpan(3) }) {
                    LeaderboardRow(board = "popularity", onBookClick = onBookClick)
                }
                item(span = { GridItemSpan(3) }) {
                    SectionHeader(title = "🎫 月票榜")
                }
                item(span = { GridItemSpan(3) }) {
                    LeaderboardRow(board = "monthly", onBookClick = onBookClick)
                }

                // ══════════════════════════════════════
                //  内容区域
                // ══════════════════════════════════════
                when {
                    state.books.isEmpty() && state.isLoading -> {
                        items(6) {
                            BookCoverSkeleton(Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    state.books.isEmpty() && state.error != null -> {
                        item(span = { GridItemSpan(3) }) {
                            AnibuzzErrorState(
                                error = state.error ?: "加载失败",
                                onRetry = { viewModel.refresh() },
                            )
                        }
                    }

                    state.books.isEmpty() -> {
                        item(span = { GridItemSpan(3) }) {
                            AnibuzzEmptyState(
                                title = if (state.query.isBlank()) "书架还没上架书哦" else "没有找到相关书籍",
                                subtitle = if (state.query.isBlank()) "快去探索精彩的新书吧～" else "换个关键词试试",
                            )
                        }
                    }

                    else -> {
                        // 封面轮播 Banner
                        if (featured.isNotEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                FeaturedCarousel(
                                    books = featured,
                                    onBookClick = onBookClick,
                                )
                            }
                        }

                        // 热门推荐（横向滚动）
                        if (popular.isNotEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                SectionHeader(title = "热门推荐")
                            }
                            item(span = { GridItemSpan(3) }) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    lazyItems(
                                        items = popular,
                                        key = { it.id ?: it.slug ?: it.title.orEmpty() },
                                    ) { book ->
                                        BookCoverCard(
                                            coverUrl = book.coverImage,
                                            title = book.title.orEmpty(),
                                            author = book.displayAuthor,
                                            wordCount = book.wordCount,
                                            onClick = { book.slug?.let(onBookClick) },
                                            modifier = Modifier.width(120.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // 最新上架（网格）
                        item(span = { GridItemSpan(3) }) {
                            SectionHeader(title = "最新上架")
                        }
                        items(
                            items = gridBooks,
                            key = { it.id ?: it.slug ?: it.title.orEmpty() },
                            span = { GridItemSpan(1) },
                        ) { book ->
                            BookCoverCard(
                                coverUrl = book.coverImage,
                                title = book.title.orEmpty(),
                                author = book.displayAuthor,
                                wordCount = book.wordCount,
                                onClick = { book.slug?.let(onBookClick) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
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

                        if (!state.canLoadMore && gridBooks.isNotEmpty()) {
                            item(span = { GridItemSpan(3) }) {
                                Text(
                                    text = "—— 到底啦 ——",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhaleColors.TextDisabled,
                                )
                            }
                        }
                    }
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

// ══════════════════════════════════════════════════════════
//  ANIBUZZ 风格组件（纯 UI 框架，无角色图）
// ══════════════════════════════════════════════════════════

/** iOS 玻璃风 Hero 卡片：磨砂玻璃容器 + 品牌文字 + CTA 按钮 */
@Composable
private fun AnibuzzHeroCard() {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        radius = GlassTokens.RadiusXL,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标签
            Text(
                text = "冲浪阅读",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTokens.SystemBlue,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(16.dp))
            // 主标题
            Text(
                text = "欢迎回来",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.Label,
            )
            Spacer(Modifier.height(8.dp))
            // 副标题
            Text(
                text = "发现属于你的下一本好书",
                style = MaterialTheme.typography.bodyLarge,
                color = GlassTokens.SecondaryLabel,
            )
            Spacer(Modifier.height(20.dp))
            // CTA 行
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                    .background(GlassTokens.GradientButton)
                    .clickable { /* TODO */ }
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

/** ANIBUZZ 风格错误状态：图标 + 文字 + 重试按钮 */
@Composable
private fun AnibuzzErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = GlassTokens.SecondaryLabel,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = WhaleColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = WhaleColors.CtaPrimary,
            ),
        ) {
            Text("重试")
        }
    }
}

/** ANIBUZZ 风格空状态：图标 + 引导文字 */
@Composable
private fun AnibuzzEmptyState(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = GlassTokens.SecondaryLabel,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = WhaleColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = WhaleColors.TextDisabled,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

/** 封面轮播 Banner：渐变遮罩 + 标题 + 状态标签 + 自动轮播 */
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
                // 渐变遮罩
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
                        StatusPill(text = book.statusText)
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

        // 页面指示点
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

/**
 * 榜单横滑行（P1）：从冲浪阅读独立后端拉取，绝不汇总主站。
 * 后端未连接 / 书库为空时显示占位，不崩溃。
 */
@Composable
private fun LeaderboardRow(
    board: String,
    onBookClick: (String) -> Unit,
) {
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(board) {
        loading = true
        entries = BackendRepository.getLeaderboard(board).getOrNull()?.entries ?: emptyList()
        loading = false
    }

    if (loading) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(5) { BookCoverSkeleton(Modifier.padding(horizontal = 4.dp)) }
        }
        return
    }

    if (entries.isEmpty()) {
        Text(
            text = "暂无数据（后端未连接或书库为空）",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = GlassTokens.SecondaryLabel,
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lazyItems(
            items = entries,
            key = { "${it.bookSourceId}:${it.bookId}" },
        ) { e ->
            BookCoverCard(
                coverUrl = e.coverUrl,
                title = e.title ?: "未知",
                author = null,
                wordCount = null,
                onClick = { onBookClick(e.bookId) },
                modifier = Modifier.width(120.dp),
            )
        }
    }
}
