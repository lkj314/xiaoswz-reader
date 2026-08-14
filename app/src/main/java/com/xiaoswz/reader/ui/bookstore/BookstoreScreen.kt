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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.update.UpdateManager
import com.xiaoswz.reader.ui.components.BookCoverCard
import com.xiaoswz.reader.ui.components.BookCoverSkeleton
import com.xiaoswz.reader.ui.components.EmptyState
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
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                // 搜索 + 筛选（常驻顶部）
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            placeholder = { Text("搜索书名 / 简介") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.refresh() }),
                            shape = RoundedCornerShape(14.dp),
                        )
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
                            // 排序切换
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

                when {
                    state.books.isEmpty() && state.isLoading -> {
                        items(6) {
                            BookCoverSkeleton(Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    state.books.isEmpty() && state.error != null -> {
                        item(span = { GridItemSpan(3) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = state.error ?: "加载失败",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.refresh() }) { Text("重试") }
                            }
                        }
                    }

                    state.books.isEmpty() -> {
                        item(span = { GridItemSpan(3) }) {
                            EmptyState(
                                icon = Icons.Default.AutoStories,
                                title = if (state.query.isBlank()) "暂无书籍" else "没有找到相关书籍",
                                subtitle = if (state.query.isBlank()) "去书城收藏喜欢的书籍吧" else "换个关键词试试",
                                modifier = Modifier.fillMaxSize(),
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

                        // 热门推荐（横向）
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
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            )
        }
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
                .height(220.dp)
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
                    model = book.coverImage,
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
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = book.displayAuthor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
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
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                        ),
                )
            }
        }
    }
}

