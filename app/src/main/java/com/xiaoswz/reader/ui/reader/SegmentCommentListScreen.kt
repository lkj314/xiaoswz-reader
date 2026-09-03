package com.xiaoswz.reader.ui.reader

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.BookSegmentCommentItem
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.xiaoswz.reader.ui.theme.MetaIcons

/** 相对时间：刚发布→「刚刚」；<60min→「x 分钟前」；<24h→「x 小时前」；更早→日期。 */
private fun formatRelativeTime(ts: Long): String {
    if (ts <= 0) return ""
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 7 * 86_400_000 -> "${diff / 86_400_000} 天前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))
    }
}

/**
 * 段评独立全屏页（v0.15.3）：跨章节聚合根段评，起点/番茄风格讨论流。
 * 列表项显示章节标题（来自本地目录映射）+ 引用快照 + 楼中楼数 + 时间；
 * 点击进入 [SegmentThreadSheet] 看单段完整线程（主楼 + 楼中楼）并可发表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentCommentListScreen(
    bookSlug: String,
    onBack: () -> Unit,
) {
    val vm: SegmentCommentListViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val thread by vm.threadState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(bookSlug) { vm.load(bookSlug) }
    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部段评") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MetaIcons.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.comments.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.comments.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (state.error != null) {
                                state.error!!
                            } else {
                                "还没有段评。在正文长按选中一段文字，点「评」即可发表段评；" +
                                    "全部段落讨论会跨章节聚合在此。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.comments, key = { it.id }) { item ->
                            SegmentCommentCard(
                                item = item,
                                chapterName = state.chapterNames[item.chapterId] ?: "未知章节",
                                onClick = {
                                    vm.openThread(
                                        chapterId = item.chapterId ?: "",
                                        paragraphIndex = item.paragraphIndex ?: 0,
                                        quote = item.quotedText ?: "",
                                    )
                                },
                            )
                        }
                        if (state.hasMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator()
                                    } else {
                                        MetaButton(text = "加载更多", onClick = { vm.loadMore() }, variant = MetaButtonVariant.Ghost)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 单段线程（主楼 + 楼中楼），复用阅读器内的 SegmentThreadSheet
    thread?.let { t ->
        SegmentThreadSheet(
            chapterId = t.chapterId,
            paragraphIndex = t.paragraphIndex,
            quote = t.quote,
            comments = t.comments,
            onDismiss = { vm.closeThread() },
            onLike = { id -> vm.likeComment(id) },
            onReport = { id -> vm.reportComment(id) },
            onPost = { content ->
                vm.postSegmentComment(t.chapterId, t.paragraphIndex, t.quote, content, null)
            },
            onReply = { pid, txt ->
                vm.postSegmentComment(t.chapterId, t.paragraphIndex, t.quote, txt, pid)
            },
        )
    }
}

@Composable
private fun SegmentCommentCard(
    item: BookSegmentCommentItem,
    chapterName: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 章节标题（来自本地目录映射）
        Text(
            text = chapterName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.padding(top = 4.dp))
        // 引用快照（名场面/名句段落）
        if (!item.quotedText.isNullOrBlank()) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "“${item.quotedText}”",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.padding(top = 6.dp))
        }
        // 主楼内容
        Text(
            text = item.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.padding(top = 6.dp))
        // 楼中楼数 + 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (item.replyCount > 0) "${item.replyCount} 条回复 ›" else "暂无回复",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatRelativeTime(item.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        HorizontalDivider()
    }
}
