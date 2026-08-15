package com.xiaoswz.reader.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.Switch
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
import com.xiaoswz.reader.ui.components.ArtImage
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
import com.xiaoswz.reader.data.sync.SyncRepository
import com.xiaoswz.reader.data.auth.AuthRepository
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAccountClick: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ReaderSettingsRepository(context.applicationContext) }
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }

    val isLoggedIn by appSettings.isLoggedInFlow.collectAsState(initial = false)
    val accountEmail by appSettings.accountEmailFlow.collectAsState(initial = null)
    val accountRole by appSettings.accountRoleFlow.collectAsState(initial = "guest")
    val roleLabel = when (accountRole) {
        "admin" -> "管理员"
        "user" -> "用户"
        else -> "游客"
    }

    var updateServerUrl by remember { mutableStateOf(BuildConfig.DEFAULT_UPDATE_SERVER) }
    var serverSaved by remember { mutableStateOf(false) }
    var crashLog by remember { mutableStateOf<String?>(null) }
    var showCrashDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateAutoCheck by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val syncRepo = remember { SyncRepository(context.applicationContext) }
    var lastSyncAt by remember { mutableStateOf(0L) }
    var autoSync by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var backendUrl by remember { mutableStateOf(BuildConfig.BACKEND_BASE_URL) }

    var cacheSizeText by remember { mutableStateOf(formatCacheSize(ChapterCacheManager.sizeBytes())) }
    var themeIndex by remember { mutableStateOf(ReaderSettings.THEME_DAY) }
    val themeNames = listOf("米纸日间", "护眼绿", "夜间模式", "纯黑 OLED")
    val appThemeMode by appSettings.themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)

    LaunchedEffect(Unit) {
        val s = repo.settingsFlow.first()
        var url = s.updateServerUrl
        // 一次性数据迁移：旧版（0.6.2 之前）的 vercel.app 更新地址在国内被 DNS 污染、
        // 0.6.3 已把 BuildConfig.DEFAULT_UPDATE_SERVER 切到 GitHub raw，
        // 但 DataStore 里持久化的旧值会覆盖默认值，导致升级后输入框仍显示旧 URL。
        // 启动检测到含 vercel.app 时自动改写到当前默认地址并回写。
        if (url.contains("vercel.app", ignoreCase = true)) {
            url = BuildConfig.DEFAULT_UPDATE_SERVER
            repo.update { it.copy(updateServerUrl = url) }
        }
        updateServerUrl = url
        themeIndex = s.themeIndex
        crashLog = CrashLogger.getLog(context)
        // 云同步状态
        lastSyncAt = appSettings.lastSyncAtFlow.first()
        autoSync = appSettings.autoSyncFlow.first()
        backendUrl = appSettings.backendBaseUrlFlow.first()
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

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        scope.launch {
                            AuthRepository.logout()
                        }
                    },
                ) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") }
            },
            title = { Text("退出登录") },
            text = { Text("退出后本机将回落为游客身份，书架与进度仍保留在本地；云同步需重新登录。") },
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
                    // 品牌头像：小鲸头像（与应用图标同步）
                    ArtImage(
                        path = "character/avatar_circle.png",
                        contentDescription = "冲浪阅读",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
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

            // ── 账号（guest / user / admin）──
            val accountSubtitle = if (isLoggedIn) {
                "已登录：${accountEmail ?: ""}（${roleLabel}）"
            } else {
                "登录后解锁评论与跨设备云同步（书架 / 进度备份）"
            }
            SettingsCard(
                icon = Icons.Default.Person,
                title = if (isLoggedIn) "账号" else "登录账号",
                subtitle = accountSubtitle,
            ) {
                if (isLoggedIn) {
                    Button(
                        onClick = { showLogoutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("退出登录")
                    }
                    // 管理员专属入口：登录为 admin 时显示，点击跳转 Web 管理后台
                    if (accountRole == "admin") {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val base = appSettings.getBackendBaseUrl().removeSuffix("/")
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("$base/admin"),
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("管理后台")
                        }
                    }
                } else {
                    Button(
                        onClick = onAccountClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("登录 / 注册")
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

            // ── 应用更新（云端优先，局域网可手填兜底）──
            SettingsCard(
                icon = Icons.Default.Cloud,
                title = "应用更新",
                subtitle = "云端更新地址（默认），也可填局域网 http://IP:端口",
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

            // ── 云同步（冲浪阅读专属后端，需登录）──
            SettingsCard(
                icon = Icons.Default.Cloud,
                title = "云同步",
                subtitle = "登录账号后备份书架与阅读进度到云端，支持跨设备恢复；离线也能读，联网后自动同步。游客暂不可用。",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "后端地址：$backendUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isLoggedIn) {
                        Text(
                            if (lastSyncAt == 0L) "上次同步：从未" else "上次同步：${formatSyncTime(lastSyncAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("启动时自动同步", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = autoSync,
                                onCheckedChange = {
                                    autoSync = it
                                    scope.launch { appSettings.setAutoSync(it) }
                                },
                            )
                        }
                        Button(
                            onClick = {
                                syncing = true
                                syncMsg = null
                                scope.launch {
                                    val res = syncRepo.syncNow()
                                    syncing = false
                                    lastSyncAt = appSettings.lastSyncAtFlow.first()
                                    syncMsg = if (res.isSuccess) {
                                        "同步成功 ✓（书架 ${res.getOrNull()?.bookshelfCount ?: 0}）"
                                    } else {
                                        res.exceptionOrNull()?.message
                                            ?: "同步失败：后端未启动或网络不可达"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !syncing,
                        ) {
                            Text(if (syncing) "同步中…" else "立即同步")
                        }
                        syncMsg?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.contains("成功")) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    } else {
                        Text(
                            "登录账号后可开启云同步：书架与阅读进度跨设备备份，换机/重装后登录同一账号即可恢复。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onAccountClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("去登录")
                        }
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

/** 同步时间格式化 */
private fun formatSyncTime(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
    return sdf.format(java.util.Date(ts))
}
