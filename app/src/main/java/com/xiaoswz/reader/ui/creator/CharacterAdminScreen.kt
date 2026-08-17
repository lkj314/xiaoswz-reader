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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.AdminBookDto
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.whaleGlassCard
import kotlinx.coroutines.launch

@Composable
fun CharacterAdminScreen(
    bookSourceId: String = "",
    bookId: String = "",
    onBack: () -> Unit,
) {
    val vm: CharacterAdminViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // 选中的书籍（来自路由或内置选书器）
    var pickedBook by remember { mutableStateOf<AdminBookDto?>(null) }

    LaunchedEffect(Unit) {
        if (bookId.isNotBlank()) {
            pickedBook = AdminBookDto(bookSourceId = bookSourceId.ifBlank { "main" }, bookId = bookId)
            vm.load(bookId)
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let { /* 由 UI 展示，clearToast 在点击后清理 */ }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (pickedBook != null) "角色录入" else "选择书籍",
                onBack = onBack,
                showLogo = false,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Toast
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
                        TextButton(onClick = { vm.clearToast() }) { Text("知道了") }
                    }
                }
            }

            if (pickedBook == null) {
                // ── 内置选书器 ──
                BookPicker(
                    onPick = { book ->
                        pickedBook = book
                        vm.load(book.bookId)
                    },
                )
            } else {
                // ── 已选书籍：角色管理 ──
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("书籍：${pickedBook!!.title ?: pickedBook!!.bookId}", color = GlassTokens.Label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("bookId: ${pickedBook!!.bookId}", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                        }
                        TextButton(onClick = { pickedBook = null; vm.dismissEditor() }) { Text("更换书籍") }
                    }
                }

                Button(
                    onClick = { vm.startAdd() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) { Text("+ 新增角色") }

                if (state.isLoading) {
                    Text("加载中…", color = GlassTokens.SecondaryLabel)
                } else if (state.characters.isEmpty()) {
                    Text("该书暂无角色，点上方按钮新增第一个角色。", color = GlassTokens.SecondaryLabel)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.characters) { ch ->
                            CharacterRow(
                                ch = ch,
                                onEdit = { vm.startEdit(ch) },
                                onDelete = { vm.delete(ch) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 角色编辑对话框 ──
    if (state.showEditor) {
        CharacterEditorDialog(
            editing = state.editing,
            saving = state.saving,
            onDismiss = { vm.dismissEditor() },
            onSave = { name, roleType, avatarUrl, description ->
                vm.save(name, roleType, avatarUrl, description)
            },
        )
    }
}

@Composable
private fun BookPicker(onPick: (AdminBookDto) -> Unit) {
    var query by remember { mutableStateOf("") }
    var books by remember { mutableStateOf<List<AdminBookDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("输入书名搜索") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Button(onClick = {
                loading = true; error = null
                scope.launch {
                    val res = BackendRepository.adminListBooks(query.takeIf { q -> q.isNotBlank() }, 1)
                    loading = false
                    if (res.isSuccess) books = res.getOrNull()?.books ?: emptyList()
                    else error = "搜索失败（请用管理员账号登录）"
                }
            }) { Text("搜索") }
        }
        Spacer(Modifier.height(12.dp))
        if (loading) Text("搜索中…", color = GlassTokens.SecondaryLabel)
        error?.let { Text(it, color = Color(0xFFE25555)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books) { b ->
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard().clickable { onPick(b) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.title ?: b.bookId, color = GlassTokens.Label, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text("bookId: ${b.bookId}", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                        }
                        if (b.hidden) Text("已隐藏", color = Color(0xFFE25555), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterRow(
    ch: com.xiaoswz.reader.data.api.AdminCharacterDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                Text(
                    ch.name,
                    color = GlassTokens.Label,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        if (ch.roleType == "main") "主角" else "配角",
                        color = GlassTokens.SystemBlue,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("❤ ${ch.heartCount}", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                }
                if (!ch.description.isNullOrBlank()) {
                    Text(ch.description, color = GlassTokens.SecondaryLabel, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Row {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFE25555)) }
            }
        }
    }
}

@Composable
private fun CharacterEditorDialog(
    editing: com.xiaoswz.reader.data.api.AdminCharacterDto?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, roleType: String, avatarUrl: String?, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var roleType by remember { mutableStateOf(editing?.roleType ?: "main") }
    var avatarUrl by remember { mutableStateOf(editing?.avatarUrl ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(name, roleType, avatarUrl, description) },
                enabled = !saving,
            ) { Text(if (saving) "保存中…" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (editing == null) "新增角色" else "编辑角色") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("角色定位", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { roleType = "main" },
                        modifier = Modifier.weight(1f),
                    ) { Text("主角", color = if (roleType == "main") Color.White else GlassTokens.Label) }
                    Button(
                        onClick = { roleType = "supporting" },
                        modifier = Modifier.weight(1f),
                    ) { Text("配角", color = if (roleType == "supporting") Color.White else GlassTokens.Label) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = { Text("头像 URL（可空，http(s)）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("简介（可空）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
    )
}
