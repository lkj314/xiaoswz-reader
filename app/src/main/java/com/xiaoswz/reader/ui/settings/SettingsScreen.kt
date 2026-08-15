package com.xiaoswz.reader.ui.settings

import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.WhaleColors
import com.xiaoswz.reader.ui.components.WhaleGlassCard
import com.xiaoswz.reader.ui.components.whaleGlassCard
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.CrashLogger
import com.xiaoswz.reader.data.cache.ChapterCacheManager
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.settings.AppThemeMode
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ReaderSettingsRepository(context.applicationContext) }
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }

    var updateServerUrl by remember { mutableStateOf(BuildConfig.DEFAULT_UPDATE_SERVER) }
    var serverSaved by remember { mutableStateOf(false) }
    var crashLog by remember { mutableStateOf<String?>(null) }
    var showCrashDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateAutoCheck by remember { mutableStateOf(false) }

    var cacheSizeText by remember { mutableStateOf(formatCacheSize(ChapterCacheManager.sizeBytes())) }
    var themeIndex by remember { mutableStateOf(ReaderSettings.THEME_DAY) }
    val themeNames = listOf("米纸日间", "护眼绿", "夜间模式", "纯黑 OLED")
    val appThemeMode by appSettings.themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)

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

    if (showUpdateDialog) {
        UpdateDialog(
            serverUrl = updateServerUrl,
            autoCheck = updateAutoCheck,
            onServerUrlChange = { newUrl ->
                updateServerUrl = newUrl
                scope.launch { repo.update { it.copy(updateServerUrl = newUrl) } }
            },
            onDismiss = { showUpdateDialog = false },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "设置",
                onBack = onBack,
                showLogo = false,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 个人资料（品牌头像 + 版本）──
            WhaleGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 品牌头像：圆形渐变背景 + 首字（Phase B 换回 avatar_circle）
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        WhaleColors.WhaleNavy,
                                        WhaleColors.WhaleBlue,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "冲",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "冲浪阅读",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            "数据源：冲浪中文网 (${BuildConfig.API_BASE_URL})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            // ── 外观（应用外壳主题）──
            SettingsCard(
                icon = Icons.Default.Palette,
                title = "外观",
                subtitle = "应用外壳浅色 / 深色，跟随系统或手动切换",
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedButton(
                        selected = appThemeMode == AppThemeMode.SYSTEM,
                        onClick = { scope.launch { appSettings.setThemeMode(AppThemeMode.SYSTEM) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    ) { Text("跟随系统") }
                    SegmentedButton(
                        selected = appThemeMode == AppThemeMode.LIGHT,
                        onClick = { scope.launch { appSettings.setThemeMode(AppThemeMode.LIGHT) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) { Text("浅色") }
                    SegmentedButton(
                        selected = appThemeMode == AppThemeMode.DARK,
                        onClick = { scope.launch { appSettings.setThemeMode(AppThemeMode.DARK) } },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    ) { Text("深色") }
                }
            }

            // ── 阅读主题（真正生效在阅读器）──
            SettingsCard(
                icon = Icons.Default.AutoStories,
                title = "阅读主题",
                subtitle = "详细排版设置请在阅读页内打开设置面板调整",
            ) {
                Column(Modifier.selectableGroup()) {
                    themeNames.forEachIndexed { idx, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = idx == themeIndex,
                                    onClick = {
                                        themeIndex = idx
                                        scope.launch { repo.update { it.copy(themeIndex = idx) } }
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

            // ── 局域网更新服务器 ──
            SettingsCard(
                icon = Icons.Default.Cloud,
                title = "局域网更新服务器",
                subtitle = "电脑端下载服务地址，格式 http://IP:端口",
            ) {
                OutlinedTextField(
                    value = updateServerUrl,
                    onValueChange = { updateServerUrl = it; serverSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    placeholder = { Text("http://192.168.2.4:8765") },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                repo.update { it.copy(updateServerUrl = updateServerUrl.trim().trimEnd('/')) }
                                serverSaved = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (serverSaved) "已保存 ✓" else "保存服务器地址")
                    }
                    Button(
                        onClick = {
                            updateAutoCheck = false
                            showUpdateDialog = true
                        },
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Text(" 检查更新")
                    }
                }
            }

            // ── 离线缓存 ──
            SettingsCard(
                icon = Icons.Default.Storage,
                title = "离线缓存",
                subtitle = "已缓存章节正文：$cacheSizeText（断网可读，随卸载清除）",
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            ChapterCacheManager.clear()
                            cacheSizeText = formatCacheSize(ChapterCacheManager.sizeBytes())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("清空离线缓存")
                }
            }

            // ── 崩溃日志 ──
            SettingsCard(
                icon = Icons.Default.BugReport,
                title = "崩溃日志",
                subtitle = if (crashLog == null) "暂无崩溃记录，应用运行正常。" else "检测到崩溃记录，点开查看完整堆栈并分享给我定位问题。",
            ) {
                if (crashLog != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

            // ── 关于 ──
            SettingsCard(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "冲浪阅读是一款原生安卓小说阅读客户端，数据全部来自冲浪中文网公开只读 API，不登录、不直连数据库。",
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            content?.let {
                Column(modifier = Modifier.padding(top = 12.dp)) { it() }
            }
        }
    }
}

/** 缓存字节数格式化：<1MB 显示 KB，否则显示 MB */
private fun formatCacheSize(bytes: Long): String {
    return if (bytes < 1024 * 1024) {
        "${bytes / 1024} KB"
    } else {
        "%.1f MB".format(bytes / 1024.0 / 1024.0)
    }
}
