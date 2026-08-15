package com.xiaoswz.reader.ui.bookshelf

import com.xiaoswz.reader.data.model.resolveCoverUrl
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    // 打开书架时再修复一次：联网按 slug 重新拉取封面写回（首页启动已先 blank 防崩）
    LaunchedEffect(Unit) {
        repo.repairCovers { slug -> ApiClient.api.getBookDetail(bookId = slug).coverUrl }
    }

    val sorted = books.sortedByDescending { it.lastReadAt }
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
                        badge = if (BookUpdateStore.getHasUpdate(book.slug)) {
                            { StatusPill(text = "有更新", containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) }
                        } else null,
                        onRemove = { scope.launch { repo.remove(book.slug) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBookCard(
    book: BookEntity,
    hasUpdate: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .liquidGlass(radius = GlassTokens.RadiusLG),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = resolveCoverUrl(book.coverUrl),
                contentDescription = book.title,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                if (hasUpdate) {
                    StatusPill(
                        text = "● 有更新",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = book.lastChapterTitle ?: "未开始阅读",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onClick) {
                    Text("继续阅读")
                }
            }
        }
    }
}
