package com.xiaoswz.reader.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.CrashLogger
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.settings.ReaderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ReaderSettingsRepository(context.applicationContext) }

    var updateServerUrl by remember { mutableStateOf(BuildConfig.DEFAULT_UPDATE_SERVER) }
    var serverSaved by remember { mutableStateOf(false) }
    var crashLog by remember { mutableStateOf<String?>(null) }
    var showCrashDialog by remember { mutableStateOf(false) }

    // 当前阅读主题（用于设置页预览/切换示例，真实生效在阅读器内）
    var themeIndex by remember { mutableStateOf(ReaderSettings.THEME_DAY) }

    val themeNames = listOf(
        "米纸日间", "护眼绿", "夜间模式", "纯黑 OLED",
    )

    LaunchedEffect(Unit) {
        val s = repo.settingsFlow.first()
        updateServerUrl = s.updateServerUrl
        themeIndex = s.themeIndex
        crashLog = CrashLogger.getLog(context)
    }

    if (showCrashDialog && crashLog != null) {
        AlertDialog(
            onDismissRequest = { showCrashDialog = false },
            confirmButton = {
                TextButton(onClick = { showCrashDialog = false }) { Text("关闭") }
            },
            title = { Text("崩溃日志") },
            text = {
                Text(
                    text = crashLog ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 版本信息 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("版本信息", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "冲浪阅读 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "数据源：冲浪中文网 (${BuildConfig.API_BASE_URL})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // ── 更新服务器配置 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("局域网更新服务器", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "电脑端下载服务地址，格式 http://IP:端口",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = updateServerUrl,
                        onValueChange = { updateServerUrl = it; serverSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        placeholder = { Text("http://192.168.2.4:8765") },
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                repo.update { it.copy(updateServerUrl = updateServerUrl.trim().trimEnd('/')) }
                                serverSaved = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(if (serverSaved) "已保存 ✓" else "保存服务器地址")
                    }
                }
            }

            // ── 崩溃日志 ──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Text(
                            " 崩溃日志",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (crashLog == null) {
                        Text(
                            "暂无崩溃记录，应用运行正常。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        Text(
                            "检测到崩溃记录，点开查看完整堆栈并分享给我定位问题。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { showCrashDialog = true }) {
                                Text("查看")
                            }
                            Button(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, crashLog)
                                        putExtra(Intent.EXTRA_SUBJECT, "冲浪阅读崩溃日志")
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "分享崩溃日志").addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK,
                                        ),
                                    )
                                },
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Text(" 分享")
                            }
                        }
                    }
                }
            }

            // ── 阅读主题（示例：真正生效在阅读器，这里仅做入口）──
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("阅读主题", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "（详细排版设置请在阅读页内打开设置面板调整）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    Column(Modifier.selectableGroup()) {
                        themeNames.forEachIndexed { idx, name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = idx == themeIndex,
                                        onClick = {
                                            themeIndex = idx
                                            scope.launch {
                                                repo.update { it.copy(themeIndex = idx) }
                                            }
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = idx == themeIndex, onClick = null)
                                Text(name, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }

            // ── 关于 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "冲浪阅读是一款原生安卓小说阅读客户端，数据全部来自冲浪中文网公开只读 API，不登录、不直连数据库。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
