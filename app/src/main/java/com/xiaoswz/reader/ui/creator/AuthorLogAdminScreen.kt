package com.xiaoswz.reader.ui.creator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.AdminBookDto
import com.xiaoswz.reader.data.api.AuthorLogDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val editorTypeLabel = listOf(
    "musings" to "碎碎念",
    "announcement" to "公告",
    "changelog" to "章节改动",
)
private val rowTypeLabel = mapOf(
    "musings" to "碎碎念",
    "announcement" to "公告",
    "changelog" to "章节改动",
)
private val rowTypeColor = mapOf(
    "musings" to Color(0xFF9B6DFF),
    "announcement" to GlassTokens.SystemBlue,
    "changelog" to Color(0xFFE0A200),
)

@Composable
fun AuthorLogAdminScreen(onBack: () -> Unit) {
    val vm: AuthorLogAdminViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { /* 初始无选中书籍，等待搜索 */ }

    Scaffold(
        topBar = { AppTopBar(title = "作者日志", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            state.toast?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassTokens.SystemBlue.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(msg, color = GlassTokens.Label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        MetaButton(text = "知道了", onClick = { vm.clearToast() }, modifier = Modifier, variant = MetaButtonVariant.Ghost)
                    }
                }
            }

            // 搜索书籍
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.searchBooks(it) },
                label = { Text("搜索书籍（书名）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 搜索结果
            if (state.bookResults.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.bookResults, key = { it.bookId }) { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectBook(book) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    book.title ?: book.bookId,
                                    color = GlassTokens.Label,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    book.author ?: "",
                                    color = GlassTokens.SecondaryLabel,
                                    fontSize = 12.sp,
                                )
                            }
                            Text("选择 ›", color = GlassTokens.SystemBlue, fontSize = 12.sp)
                        }
                        androidx.compose.material3.HorizontalDivider(color = GlassTokens.GlassFillStrong)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.selectedBookId == null) {
                Text("先搜索并选择一本书，再管理它的作者日志。", color = GlassTokens.SecondaryLabel)
            } else {
                Text(
                    "当前书籍：${state.selectedBookTitle ?: state.selectedBookId}",
                    color = GlassTokens.Label,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                MetaButton(
                    text = "+ 新建日志",
                    onClick = { vm.startAdd() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )

                if (state.isLoading) {
                    Text("加载中…", color = GlassTokens.SecondaryLabel)
                } else if (state.logs.isEmpty()) {
                    Text("这本书还没有作者日志，点上方按钮写第一条吧。", color = GlassTokens.SecondaryLabel)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.logs, key = { it.id }) { log ->
                            AuthorLogAdminRow(
                                log = log,
                                onEdit = { vm.startEdit(log) },
                                onDelete = { vm.delete(log) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showEditor) {
        AuthorLogEditorDialog(
            editing = state.editing,
            saving = state.saving,
            onDismiss = { vm.dismissEditor() },
            onSave = { type, title, body, chapterRef, pinned ->
                vm.save(type, title, body, chapterRef, pinned)
            },
        )
    }
}

@Composable
private fun AuthorLogAdminRow(
    log: AuthorLogDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rowTypeLabel[log.type] ?: log.type,
                        color = rowTypeColor[log.type] ?: GlassTokens.SystemBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (log.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Text("置顶", color = GlassTokens.TertiaryLabel, fontSize = 11.sp)
                    }
                    if (!log.chapterRef.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text("📖 ${log.chapterRef}", color = GlassTokens.SecondaryLabel, fontSize = 11.sp)
                    }
                }
                Text(log.title, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                Text(log.body, color = GlassTokens.SecondaryLabel, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Text(fmt.format(Date(log.createdAt)), color = GlassTokens.TertiaryLabel, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Row {
                MetaButton(text = "编辑", onClick = onEdit, modifier = Modifier, variant = MetaButtonVariant.Ghost)
                MetaButton(text = "删除", onClick = onDelete, modifier = Modifier, variant = MetaButtonVariant.Ghost)
            }
        }
    }
}

@Composable
private fun AuthorLogEditorDialog(
    editing: AuthorLogDto?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (type: String, title: String, body: String, chapterRef: String?, pinned: Boolean) -> Unit,
) {
    var type by remember { mutableStateOf(editing?.type ?: "musings") }
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var body by remember { mutableStateOf(editing?.body ?: "") }
    var chapterRef by remember { mutableStateOf(editing?.chapterRef ?: "") }
    var pinned by remember { mutableStateOf(editing?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(type, title, body, chapterRef.trim().ifEmpty { null }, pinned) },
                enabled = !saving,
            ) { Text(if (saving) "保存中…" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (editing == null) "新建作者日志" else "编辑作者日志") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 类型
                Text("类型", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    editorTypeLabel.forEach { (tv, label) ->
                        Button(
                            onClick = { type = tv },
                            modifier = Modifier.weight(1f),
                        ) { Text(label, color = if (type == tv) Color.White else GlassTokens.Label) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("正文 *") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = chapterRef, onValueChange = { chapterRef = it }, label = { Text("关联章节（选填，如：第12章 迷雾）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                // 置顶
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("置顶（钉在最前）", color = GlassTokens.Label)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
    )
}
