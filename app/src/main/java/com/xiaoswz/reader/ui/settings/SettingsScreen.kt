package com.xiaoswz.reader.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.xiaoswz.reader.ui.theme.GlassTokens
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
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onUserCenterClick: () -> Unit = {},
    onCreatorClick: () -> Unit = {},
) {
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

    var cacheSizeText by remember { mutableStateOf(formatCacheSize(ChapterCacheManager.sizeBytes())) }
    var themeIndex by remember { mutableStateOf(ReaderSettings.THEME_DAY) }
    val themeNames = listOf("米纸日间", "护眼绿", "夜间模式", "纯黑 OLED")
    // 预读 / 连续阅读开关（0.9.2 / 0.9.3）
    var prefetchNext by remember { mutableStateOf(3) }
    var prefetchPrev by remember { mutableStateOf(1) }
    var continuousScroll by remember { mutableStateOf(true) }
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
        prefetchNext = s.prefetchNext
        prefetchPrev = s.prefetchPrev
        continuousScroll = s.continuousScroll
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

            // ── 账户摘要（点击进入用户中心）──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .whaleGlassCard()
                    .clickable { onUserCenterClick() },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (isLoggedIn) (accountEmail ?: "读者") else "游客",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassTokens.Label,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (isLoggedIn) "身份：$roleLabel · 点击管理账号与云同步" else "点击登录 / 注册",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.SecondaryLabel,
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(18.dp))
                }
            }

            // ── 创作者中心（仅管理员可见：App 内嵌管理模块入口）──
            if (accountRole == "admin") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .whaleGlassCard()
                        .clickable { onCreatorClick() },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "创作者中心",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GlassTokens.Label,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "角色录入 · 书籍编辑 · 公告管理",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(18.dp))
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
                Spacer(Modifier.height(12.dp))
                // 向后预读深度（0=关闭）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("向后预读", style = MaterialTheme.typography.bodyMedium)
                    Text("${prefetchNext} 章", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = prefetchNext.toFloat(),
                    onValueChange = {
                        prefetchNext = it.toInt().coerceIn(0, 8)
                        scope.launch { repo.update { s -> s.copy(prefetchNext = prefetchNext) } }
                    },
                    valueRange = 0f..8f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth(),
                )
                // 向前预读深度（0=关闭）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("向前预读", style = MaterialTheme.typography.bodyMedium)
                    Text("${prefetchPrev} 章", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = prefetchPrev.toFloat(),
                    onValueChange = {
                        prefetchPrev = it.toInt().coerceIn(0, 4)
                        scope.launch { repo.update { s -> s.copy(prefetchPrev = prefetchPrev) } }
                    },
                    valueRange = 0f..4f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                // 连续无缝滚动（原生阅读体验：所有章节拼成一条长文，下滑直接衔接下一章）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("连续无缝滚动", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = continuousScroll,
                        onCheckedChange = {
                            continuousScroll = it
                            scope.launch { repo.update { s -> s.copy(continuousScroll = it) } }
                        },
                    )
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
