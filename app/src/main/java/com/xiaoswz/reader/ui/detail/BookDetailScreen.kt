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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.xiaoswz.reader.data.model.formatWordCount
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.bookshelf.BookEntity

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

    LaunchedEffect(slug) {
        viewModel.load(slug)
        collected = bookshelfRepo.isCollected(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.detail?.name ?: "书籍详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.retry(slug) }) {
                        Text("重试")
                    }
                }
            }

            else -> {
                val detail = state.detail ?: return@Scaffold
                val chapters = detail.chapters.orEmpty()

                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    // 书籍信息头部
                    item {
                        Row(modifier = Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = detail.coverUrl,
                                contentDescription = detail.name,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = detail.name.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = detail.author.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = buildList {
                                        if (!detail.status.isNullOrBlank()) add(detail.status)
                                        add(formatWordCount(detail.wordCount))
                                        add("${detail.chapterCount ?: chapters.size}章")
                                    }.filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val firstChapter = chapters.firstOrNull()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { firstChapter?.id?.let(onChapterClick) },
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
                                                            coverUrl = detail.coverUrl,
                                                            firstChapterId = firstChapter?.id,
                                                            lastChapterId = firstChapter?.id,
                                                            lastChapterTitle = firstChapter?.name,
                                                            addedAt = System.currentTimeMillis(),
                                                            lastReadAt = System.currentTimeMillis(),
                                                        )
                                                    )
                                                    collected = true
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

                    // 简介
                    if (!detail.intro.isNullOrBlank()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = "简介",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = detail.intro,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
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
                                .clickable { chapter.id?.let(onChapterClick) }
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
