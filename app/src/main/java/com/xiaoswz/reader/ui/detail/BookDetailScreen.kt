package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.xiaoswz.reader.data.model.formatWordCount
import com.xiaoswz.reader.data.model.resolveCoverUrl
import com.xiaoswz.reader.data.model.shrinkCover
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.bookshelf.BookEntity
import com.xiaoswz.reader.data.bookshelf.BookUpdateStore
import com.xiaoswz.reader.ui.components.StatusPill
import com.xiaoswz.reader.ui.components.AppTopBar

private fun statusText(s: String?): String = when (s) {
    "ONGOING" -> "连载中"
    "COMPLETED" -> "已完结"
    "HIATUS" -> "暂停中"
    "DROPPED" -> "已切书"
    else -> s ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    slug: String,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    viewModel: BookDetailViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookshelfRepo = remember { BookshelfRepository(context.applicationContext) }
    var collected by remember { mutableStateOf(false) }
    var hasUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        viewModel.load(slug)
        collected = bookshelfRepo.isCollected(slug)
    }

    LaunchedEffect(state.detail) {
        val d = state.detail ?: return@LaunchedEffect
        val known = BookUpdateStore.getKnown(slug)
        if (known != null && (d.chapterCount ?: 0) > known) {
            BookUpdateStore.markUpdated(slug)
        }
        hasUpdate = BookUpdateStore.getHasUpdate(slug)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.detail?.name ?: "书籍详情",
                onBack = onBack,
                showLogo = false,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error ?: "加载失败",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.retry(slug) }) {
                        Text("重试")
                    }
                }
            }

            else -> {
                val detail = state.detail ?: return@Scaffold
                val chapters = detail.chapters.orEmpty()
                val currentCount = detail.chapterCount ?: chapters.size
                val openChapter: (String?) -> Unit = { id ->
                    BookUpdateStore.markSeen(slug, currentCount)
                    hasUpdate = false
                    id?.let(onChapterClick)
                }

                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    // 书籍信息头部（渐变背景）
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.background,
                                        ),
                                    ),
                                )
                                .padding(16.dp),
                        ) {
                            Row {
                                AsyncImage(
                                    model = resolveCoverUrl(detail.coverUrl),
                                    contentDescription = detail.name,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = detail.name.orEmpty(),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = detail.author.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        val st = statusText(detail.status)
                                        if (st.isNotBlank()) {
                                            StatusPill(text = st)
                                        }
                                        if (hasUpdate) {
                                            StatusPill(
                                                text = "有更新",
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        StatusPill(
                                            text = formatWordCount(detail.wordCount),
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        StatusPill(
                                            text = "${detail.chapterCount ?: chapters.size}章",
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    val firstChapter = chapters.firstOrNull()
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Button(
                                            onClick = { openChapter(firstChapter?.id) },
                                            enabled = firstChapter != null,
                                        ) {
                                            Text("开始阅读")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    if (collected) {
                                                        bookshelfRepo.remove(slug)
                                                        collected = false
                                                    } else {
                                                        bookshelfRepo.add(
                                                            BookEntity(
                                                                slug = slug,
                                                                title = detail.name ?: "",
                                                                author = detail.author,
                                                                coverUrl = shrinkCover(detail.coverUrl),
                                                                firstChapterId = firstChapter?.id,
                                                                lastChapterId = firstChapter?.id,
                                                                lastChapterTitle = firstChapter?.name,
                                                                addedAt = System.currentTimeMillis(),
                                                                lastReadAt = System.currentTimeMillis(),
                                                            )
                                                        )
                                                        collected = true
                                                        BookUpdateStore.setKnown(slug, currentCount)
                                                    }
                                                }
                                            },
                                        ) {
                                            Text(if (collected) "移出书架" else "加入书架")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 简介
                    if (!detail.intro.isNullOrBlank()) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "简介",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = detail.intro,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // 目录标题
                    item {
                        HorizontalDivider()
                        Text(
                            text = "目录（共 ${chapters.size} 章）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    // 章节列表
                    items(
                        items = chapters,
                        key = { it.id ?: it.index ?: 0 },
                    ) { chapter ->
                        Text(
                            text = chapter.name.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openChapter(chapter.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
