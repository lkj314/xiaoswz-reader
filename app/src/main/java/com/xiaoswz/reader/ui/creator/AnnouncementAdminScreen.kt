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
import com.xiaoswz.reader.data.api.AdminAnnouncementDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.whaleGlassCard
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val levelLabel = mapOf("info" to "信息", "warning" to "警告", "important" to "重要")
private val levelColor = mapOf(
    "info" to GlassTokens.SystemBlue,
    "warning" to Color(0xFFE0A200),
    "important" to Color(0xFFE25555),
)

@Composable
fun AnnouncementAdminScreen(onBack: () -> Unit) {
    val vm: AnnouncementAdminViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = { AppTopBar(title = "公告管理", onBack = onBack, showLogo = false) },
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
                        TextButton(onClick = { vm.clearToast() }) { Text("知道了") }
                    }
                }
            }

            Button(onClick = { vm.startAdd() }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("+ 新建公告")
            }

            if (state.isLoading) {
                Text("加载中…", color = GlassTokens.SecondaryLabel)
            } else if (state.announcements.isEmpty()) {
                Text("暂无公告。", color = GlassTokens.SecondaryLabel)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.announcements) { a ->
                        AnnouncementRow(
                            a = a,
                            onEdit = { vm.startEdit(a) },
                            onDelete = { vm.delete(a) },
                        )
                    }
                }
            }
        }
    }

    if (state.showEditor) {
        AnnouncementEditorDialog(
            editing = state.editing,
            saving = state.saving,
            onDismiss = { vm.dismissEditor() },
            onSave = { title, body, level -> vm.save(title, body, level) },
        )
    }
}

@Composable
private fun AnnouncementRow(
    a: AdminAnnouncementDto,
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
                    Text(a.title, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        levelLabel[a.level] ?: a.level,
                        color = levelColor[a.level] ?: GlassTokens.SystemBlue,
                        fontSize = 12.sp,
                    )
                }
                Text(a.body, color = GlassTokens.SecondaryLabel, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Text(fmt.format(Date(a.publishedAt)), color = GlassTokens.TertiaryLabel, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Row {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFE25555)) }
            }
        }
    }
}

@Composable
private fun AnnouncementEditorDialog(
    editing: AdminAnnouncementDto?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String, level: String) -> Unit,
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var body by remember { mutableStateOf(editing?.body ?: "") }
    var level by remember { mutableStateOf(editing?.level ?: "info") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(title, body, level) },
                enabled = !saving,
            ) { Text(if (saving) "保存中…" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (editing == null) "新建公告" else "编辑公告") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("正文 *") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(10.dp))
                Text("级别", color = GlassTokens.SecondaryLabel, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("info" to "信息", "warning" to "警告", "important" to "重要").forEach { (lv, label) ->
                        Button(
                            onClick = { level = lv },
                            modifier = Modifier.weight(1f),
                        ) { Text(label, color = if (level == lv) Color.White else GlassTokens.Label) }
                    }
                }
            }
        },
    )
}
