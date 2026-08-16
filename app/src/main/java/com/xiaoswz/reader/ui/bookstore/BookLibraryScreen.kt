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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.BookRepository
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.api.LeaderboardEntry
import com.xiaoswz.reader.data.model.BookDto
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.components.BookCoverCard
import com.xiaoswz.reader.ui.components.BookCoverSkeleton
import com.xiaoswz.reader.ui.components.SectionHeader
import kotlinx.coroutines.launch

private const val STATUS_ALL = "all"
private const val STATUS_ONGOING = "ONGOING"
private const val STATUS_COMPLETED = "COMPLETED"

/**
 * 书库（0.8.2 由「书城」拆出）：集中承载发现类操作——
 * 分区浏览（category）/ 状态筛选 / 排序 / 搜索 / 分页，以及从首页迁移来的
 * 排行榜快照（热榜 / 月票榜）与「最新上架」网格。不在底部导航新增 tab，仅由首页入口进入。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun BookLibraryScreen(
    onBookClick: (String) -> Unit,
    viewModel: BookLibraryViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // 分区（分类）列表：来自冲浪阅读自有 BookCatalog，绝不碰主站
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        categories = BookRepository().getCatalogCategories().getOrNull() ?: emptyList()
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

    val gridHeader = if (state.sort == BookLibraryUiState.SORT_POPULAR) "🔥 热门作品" else "📚 最新上架"

    Scaffold(
        topBar = {
            AppTopBar(title = "书库")
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
                //  搜索栏
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            placeholder = {
                                Text("搜索书名 / 简介", color = GlassTokens.SecondaryLabel)
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
                    }
                }

                // ══════════════════════════════════════
                //  分区浏览（分类筛选）—— 0.8.2 新增
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryChip("全部", state.category == "all") {
                            viewModel.onCategoryChange("all")
                        }
                        categories.forEach { cat ->
                            CategoryChip(cat, state.category == cat) {
                                viewModel.onCategoryChange(cat)
                            }
                        }
                    }
                }

                // ══════════════════════════════════════
                //  状态筛选 + 排序
                // ══════════════════════════════════════
                item(span = { GridItemSpan(3) }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            selected = state.sort == BookLibraryUiState.SORT_LATEST,
                            onClick = { viewModel.onSortChange(BookLibraryUiState.SORT_LATEST) },
                            label = { Text("最新") },
                        )
                        FilterChip(
                            selected = state.sort == BookLibraryUiState.SORT_POPULAR,
                            onClick = { viewModel.onSortChange(BookLibraryUiState.SORT_POPULAR) },
                            label = { Text("热门") },
                        )
                    }
                }

                // ══════════════════════════════════════
                //  排行榜快照（由首页迁移而来，冲浪阅读自有后端，绝不汇总主站）
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
                //  最新上架 / 全部作品 网格
                // ══════════════════════════════════════
                when {
                    statusFiltered.isEmpty() && state.isLoading -> {
                        items(6) {
                            BookCoverSkeleton(Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    statusFiltered.isEmpty() && state.error != null -> {
                        item(span = { GridItemSpan(3) }) {
                            LibraryErrorState(
                                error = state.error ?: "加载失败",
                                onRetry = { viewModel.refresh() },
                            )
                        }
                    }

                    statusFiltered.isEmpty() -> {
                        item(span = { GridItemSpan(3) }) {
                            LibraryEmptyState(
                                title = if (state.query.isBlank()) "书库还没有上架书哦" else "没有找到相关书籍",
                                subtitle = if (state.query.isBlank()) "快去探索精彩的新书吧～" else "换个关键词或分区试试",
                            )
                        }
                    }

                    else -> {
                        item(span = { GridItemSpan(3) }) {
                            SectionHeader(title = gridHeader)
                        }
                        items(
                            items = statusFiltered,
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

                        if (!state.canLoadMore && statusFiltered.isNotEmpty()) {
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

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun LibraryErrorState(
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

@Composable
private fun LibraryEmptyState(
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

/**
 * 榜单横滑行：从冲浪阅读独立后端拉取，绝不汇总主站。
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
                coverUrl = e.coverUrl ?: e.coverDataUri,
                title = e.title ?: "未知",
                author = null,
                wordCount = null,
                onClick = { onBookClick(e.bookId) },
                modifier = Modifier.width(120.dp),
            )
        }
    }
}
