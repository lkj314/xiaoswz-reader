package com.xiaoswz.reader.ui.reader

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.CommentItem
import com.xiaoswz.reader.data.api.SegmentCommentItem

/** 共享：单条评论行（章评 / 段评通用）。quote 为段评引用快照，可空。 */
@Composable
private fun CommentRow(
    content: String,
    likeCount: Int,
    quote: String? = null,
    onLike: () -> Unit,
    onReport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (!quote.isNullOrBlank()) {
            Text(
                text = "“${quote}”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onLike) { Text("赞 $likeCount") }
            TextButton(onClick = onReport) { Text("举报") }
        }
        HorizontalDivider()
    }
}

/** 共享：评论输入条（胶囊风，登录后显示；失败由 VM Toast 反馈）。 */
@Composable
private fun CommentInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("说点什么…") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text.trim())
                    text = ""
                }
            },
        ) { Text("发送") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterCommentSheet(
    bookSlug: String,
    chapterId: String,
    onDismiss: () -> Unit,
) {
    val vm: ChapterCommentViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(bookSlug, chapterId) { vm.load(bookSlug, chapterId) }
    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("章评（${state.commentTotal}）", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(state.comments, key = { it.id }) { c ->
                        CommentRow(
                            content = c.content,
                            likeCount = c.likeCount,
                            onLike = { vm.likeComment(c.id) },
                            onReport = { vm.reportComment(c.id) },
                        )
                    }
                }
            }
            CommentInputBar(onSend = vm::postComment)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentCommentSheet(
    segmentComments: List<SegmentCommentItem>,
    onDismiss: () -> Unit,
    onOpenThread: (paragraphIndex: Int, quote: String) -> Unit,
) {
    val groups = segmentComments.filter { it.paragraphIndex != null }
        .groupBy { it.paragraphIndex!! }
        .toSortedMap()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("段评", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (groups.isEmpty()) {
                Text(
                    "还没有段评。在正文长按选中一段文字，点「评」即可发表段评。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(groups.entries.toList(), key = { it.key }) { (pIdx, list) ->
                        val quote = list.firstOrNull()?.quotedText ?: list.firstOrNull()?.content ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenThread(pIdx, quote) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "“${quote.take(22)}”",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "${list.size} 条",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentThreadSheet(
    chapterId: String,
    paragraphIndex: Int,
    quote: String,
    comments: List<SegmentCommentItem>,
    onDismiss: () -> Unit,
    onLike: (String) -> Unit,
    onReport: (String) -> Unit,
    onPost: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("本段段评", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "“${quote}”",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(comments, key = { it.id }) { c ->
                    CommentRow(
                        content = c.content,
                        likeCount = c.likeCount,
                        onLike = { onLike(c.id) },
                        onReport = { onReport(c.id) },
                    )
                }
            }
            CommentInputBar(onSend = onPost)
        }
    }
}

@Composable
fun SegmentComposeDialog(
    anchor: SegmentAnchor,
    onDismiss: () -> Unit,
    onSend: (content: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        onDismiss()
                    }
                },
            ) { Text("发表") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("发表段评") },
        text = {
            Column {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "“${anchor.quotedText}”",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("说点什么…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
            }
        },
    )
}
