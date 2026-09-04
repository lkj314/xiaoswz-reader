package com.xiaoswz.reader.ui.creator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.AdminBookDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import androidx.compose.runtime.collectAsState

@Composable
fun BookAdminScreen(
    onBack: () -> Unit,
    onManageCharacters: (src: String, id: String) -> Unit = { _, _ -> },
) {
    val vm: BookAdminViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.search("") }

    Scaffold(
        topBar = { AppTopBar(title = "书籍编辑", onBack = onBack, showLogo = false) },
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

            // 搜索栏
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.onQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("输入书名搜索（留空看热门）") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                MetaButton(text = "搜索", onClick = { vm.search() }, modifier = Modifier)
            }
            Spacer(Modifier.height(8.dp))
            Text("共 ${state.total} 本", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                Text("加载中…", color = GlassTokens.SecondaryLabel)
            } else if (state.books.isEmpty()) {
                Text("没有匹配的书籍。", color = GlassTokens.SecondaryLabel)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.books) { b ->
                        BookRow(
                            book = b,
                            onEdit = { vm.startEdit(b) },
                            onManageCharacters = { onManageCharacters(b.bookSourceId, b.bookId) },
                        )
                    }
                    if (state.books.size >= 30) {
                        item {
                            MetaButton(
                                text = "下一页",
                                onClick = { vm.nextPage() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.editing != null) {
        BookEditorDialog(
            book = state.editing!!,
            saving = state.saving,
            onDismiss = { vm.dismissEditor() },
            onSave = { title, author, coverUrl, hidden ->
                vm.save(title, author, coverUrl, hidden)
            },
        )
    }
}

@Composable
private fun BookRow(
    book: AdminBookDto,
    onEdit: () -> Unit,
    onManageCharacters: () -> Unit,
) {
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
                Text(book.title ?: book.bookId, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                Text("bookId: ${book.bookId}", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                if (book.hidden) Text("已隐藏", color = Color(0xFFE25555), fontSize = 12.sp)
            }
            Row {
                MetaButton(text = "角色", onClick = onManageCharacters, modifier = Modifier, variant = MetaButtonVariant.Ghost)
                MetaButton(text = "编辑", onClick = onEdit, modifier = Modifier, variant = MetaButtonVariant.Ghost)
            }
        }
    }
}

@Composable
private fun BookEditorDialog(
    book: AdminBookDto,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, coverUrl: String, hidden: Boolean) -> Unit,
) {
    var title by remember { mutableStateOf(book.title ?: "") }
    var author by remember { mutableStateOf(book.author ?: "") }
    var coverUrl by remember { mutableStateOf(book.coverUrl ?: "") }
    var hidden by remember { mutableStateOf(book.hidden) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(title, author, coverUrl, hidden) },
                enabled = !saving,
            ) { Text(if (saving) "保存中…" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("编辑书籍元数据") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("书名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("作者") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = coverUrl, onValueChange = { coverUrl = it }, label = { Text("封面 URL（http(s)）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Switch(checked = hidden, onCheckedChange = { hidden = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (hidden) "已隐藏（不出现在榜单/书库）" else "展示中", color = GlassTokens.Label)
                }
                Spacer(Modifier.height(6.dp))
                Text("仅修改聚合元数据，绝不触碰小说正文。", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
            }
        },
    )
}
