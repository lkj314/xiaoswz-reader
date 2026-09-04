package com.xiaoswz.reader.ui.bookshelf
import com.xiaoswz.reader.ui.components.MetaCoverImage

import com.xiaoswz.reader.ui.components.MetaButton
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.data.bookshelf.BookEntity
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.bookshelf.BookUpdateStore
import com.xiaoswz.reader.ui.components.BookCoverCard
import com.xiaoswz.reader.ui.components.EmptyState
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.StatusPill
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.liquidGlass
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.xiaoswz.reader.data.api.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onBookClick: (slug: String, chapterId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { BookshelfRepository(context.applicationContext) }
    val books by repo.observeAll().collectAsState(initial = emptyList())
    // 封面修复只在 AppRoot 启动时执行一次（0.20.4 性能修复）。
    // 旧代码在这里每次打开书架都再跑一遍 repairCovers —— 整表 UPDATE + 逐本联网，
    // 与启动那次完全重复，是书架打开卡顿的直接原因。已移除。

    // 状态筛选（0.16.0）：全部 / 在读 / 读完 / 想读
    var filter by remember { mutableStateOf("all") }
    var statusTarget by remember { mutableStateOf<BookEntity?>(null) }

    val shown = if (filter == "all") books else books.filter { it.status == filter }
    val sorted = shown.sortedByDescending { it.lastReadAt }
    val hero = sorted.firstOrNull()
    val rest = sorted.drop(1)

    val openBook: (BookEntity) -> Unit = { book ->
        BookUpdateStore.clearUpdate(book.slug)
        val target = book.lastChapterId ?: book.firstChapterId
        if (target != null) onBookClick(book.slug, target)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "我的书架",
                showLogo = true,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                EmptyState(
                    title = "书架还是空的",
                    subtitle = "去书城收藏喜欢的书籍吧",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 状态筛选（0.16.0）：全部 / 在读 / 读完 / 想读
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("全部") })
                        FilterChip(selected = filter == "reading", onClick = { filter = "reading" }, label = { Text("在读") })
                        FilterChip(selected = filter == "finished", onClick = { filter = "finished" }, label = { Text("读完") })
                        FilterChip(selected = filter == "plan", onClick = { filter = "plan" }, label = { Text("想读") })
                    }
                }

                // 欢迎头：iOS 玻璃风小卡片
                item(span = { GridItemSpan(3) }) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        radius = GlassTokens.RadiusMD,
                    ) {
                        Text(
                            text = "欢迎回来，继续你的阅读之旅",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = GlassTokens.Label,
                        )
                    }
                }

                // 最近阅读大卡
                if (hero != null) {
                    item(span = { GridItemSpan(3) }) {
                        HeroBookCard(
                            book = hero,
                            hasUpdate = BookUpdateStore.getHasUpdate(hero.slug),
                            progress = hero.progressPercent,
                            statusLabel = statusLabelOf(hero.status),
                            onClick = { openBook(hero) },
                        )
                    }
                }

                // 封面网格
                items(
                    items = rest,
                    key = { it.slug },
                    span = { GridItemSpan(1) },
                ) { book ->
                    BookCoverCard(
                        coverUrl = book.coverUrl,
                        title = book.title,
                        author = book.author,
                        onClick = { openBook(book) },
                        progress = book.progressPercent.takeIf { it > 0 },
                        statusLabel = statusLabelOf(book.status),
                        onLongClick = { statusTarget = book },
                        badge = if (BookUpdateStore.getHasUpdate(book.slug)) {
                            { StatusPill(text = "有更新", containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) }
                        } else null,
                        onRemove = { scope.launch { repo.remove(book.slug) } },
                    )
                }
            }
        }
    }

    // 长按卡片 → 设置阅读状态（0.16.0）
    statusTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { statusTarget = null },
            confirmButton = { },
            title = { Text("设置阅读状态", color = GlassTokens.Label) },
            text = {
                Column {
                    listOf("reading" to "在读", "finished" to "读完", "plan" to "想读").forEach { (value, label) ->
                        TextButton(onClick = {
                            statusTarget = null
                            scope.launch { repo.setStatus(book.slug, value) }
                        }) { Text(label, color = GlassTokens.Label) }
                    }
                }
            },
            containerColor = GlassTokens.GroupedBackground,
        )
    }
}

@Composable
private fun HeroBookCard(
    book: BookEntity,
    hasUpdate: Boolean,
    progress: Int,
    statusLabel: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .liquidGlass(radius = GlassTokens.RadiusLG),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            MetaCoverImage(
                model = book.coverUrl,
                title = book.title,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.author ?: "佚名",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasUpdate) {
                        StatusPill(
                            text = "● 有更新",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (statusLabel != null) {
                        StatusPill(text = statusLabel)
                    }
                }
                if (hasUpdate || statusLabel != null) Spacer(Modifier.height(8.dp))
                Text(
                    text = book.lastChapterTitle ?: "未开始阅读",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "已读 $progress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                MetaButton(text = "继续阅读", onClick = onClick)
            }
        }
    }
}

/** 状态角标文字：在读不显示（避免网格噪音），读完/想读显示；未知状态返回 null。 */
private fun statusLabelOf(status: String): String? = when (status) {
    "finished" -> "读完"
    "plan" -> "想读"
    else -> null
}
