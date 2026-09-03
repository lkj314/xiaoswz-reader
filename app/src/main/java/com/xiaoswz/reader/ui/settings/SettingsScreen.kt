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
import androidx.compose.ui.res.painterResource
import com.xiaoswz.reader.R
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
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
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
import com.xiaoswz.reader.data.cache.BookMetaCache
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.settings.AppThemeMode
import com.xiaoswz.reader.data.settings.ReaderSettingsRepository
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.ui.update.UpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // 修复：缓存大小/条目数需要遍历上千个文件，原先在组合阶段同步执行 → 卡主线程。
    // 改为先给占位值，再由 LaunchedEffect 切到 Dispatchers.IO 异步算出后回填。
    var cacheSizeText by remember { mutableStateOf(formatCacheSize(0L)) }
    var chapterCount by remember { mutableStateOf(0) }
    var metaCacheSizeText by remember { mutableStateOf(formatCacheSize(0L)) }
    var prefetchWifiOnly by remember { mutableStateOf(false) }
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
        prefetchWifiOnly = s.prefetchWifiOnly
        crashLog = CrashLogger.getLog(context)
    }

    // 缓存统计：遍历上千个缓存文件属重 IO，统一切到 Dispatchers.IO；
    // 清空缓存后复用下面两个函数刷新，保证「计算」与「删除」都不在主线程。
    suspend fun refreshChapterCacheStats() {
        val size = withContext(Dispatchers.IO) { ChapterCacheManager.sizeBytes() }
        val count = withContext(Dispatchers.IO) { ChapterCacheManager.entryCount() }
        cacheSizeText = formatCacheSize(size)
        chapterCount = count
    }

    suspend fun refreshMetaCacheStats() {
        val size = withContext(Dispatchers.IO) { BookMetaCache.sizeBytes() }
        metaCacheSizeText = formatCacheSize(size)
    }

    // 进入设置页后异步统计缓存占用（组合阶段不再做文件 IO）
    LaunchedEffect(Unit) {
        refreshChapterCacheStats()
        refreshMetaCacheStats()
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
                        Icon(painterResource(R.drawable.ic_meta_person), null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(28.dp))
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
                    Icon(painterResource(R.drawable.ic_meta_chevron_right), null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(18.dp))
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
                            Icon(painterResource(R.drawable.ic_meta_edit), null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(28.dp))
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
                        Icon(painterResource(R.drawable.ic_meta_chevron_right), null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── 外观（应用外壳主题）──
            SettingsCard(
                icon = R.drawable.ic_meta_palette,
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
                icon = R.drawable.ic_meta_book_open,
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
                icon = R.drawable.ic_meta_cloud,
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
                    MetaButton(
                        text = if (serverSaved) "已保存 ✓" else "保存服务器地址",
                        onClick = {
                            scope.launch {
                                repo.update { it.copy(updateServerUrl = updateServerUrl.trim().trimEnd('/')) }
                                serverSaved = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MetaButton(
                        onClick = {
                            updateAutoCheck = false
                            showUpdateDialog = true
                        },
                        modifier = Modifier,
                        variant = MetaButtonVariant.Outline,
                    ) {
                        Icon(painterResource(R.drawable.ic_meta_update), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" 检查更新")
                    }
                }
            }

            // ── 离线缓存 ──
            SettingsCard(
                icon = R.drawable.ic_meta_storage,
                title = "离线缓存",
                subtitle = "章节正文：$cacheSizeText（$chapterCount 章）· 书籍元数据：$metaCacheSizeText（断网可读，随卸载清除）",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaButton(
                        text = "清空章节缓存",
                        onClick = {
                            scope.launch {
                                // 修复：删除上千个文件 + 重新统计体积，全部切到 IO 线程
                                withContext(Dispatchers.IO) { ChapterCacheManager.clear() }
                                refreshChapterCacheStats()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    MetaButton(
                        text = "清空书籍元数据",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { BookMetaCache.clear() }
                                refreshMetaCacheStats()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                MetaButton(
                    text = "清空全部离线缓存",
                    onClick = {
                        scope.launch {
                            // 修复：清空全部缓存同样切到 IO 线程，避免主线程删除上千文件导致卡顿/ANR
                            withContext(Dispatchers.IO) {
                                ChapterCacheManager.clear()
                                BookMetaCache.clear()
                            }
                            refreshChapterCacheStats()
                            refreshMetaCacheStats()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // 仅 WiFi 预加载（流量敏感）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("仅 WiFi 预加载", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = prefetchWifiOnly,
                        onCheckedChange = {
                            prefetchWifiOnly = it
                            scope.launch { repo.update { s -> s.copy(prefetchWifiOnly = it) } }
                        },
                    )
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
                icon = R.drawable.ic_meta_bug,
                title = "崩溃日志",
                subtitle = if (crashLog == null) "暂无崩溃记录，应用运行正常。" else "检测到崩溃记录，点开查看完整堆栈并分享给我定位问题。",
            ) {
                if (crashLog != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetaButton(text = "查看", onClick = { showCrashDialog = true })
                        MetaButton(
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
                            modifier = Modifier,
                        ) {
                            Icon(painterResource(R.drawable.ic_meta_share), contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" 分享")
                        }
                    }
                }
            }

            // ── 关于 ──
            SettingsCard(
                icon = R.drawable.ic_meta_info,
                title = "关于",
                subtitle = "冲浪阅读是一款原生安卓小说阅读客户端，数据全部来自冲浪中文网公开只读 API，不登录、不直连数据库。",
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: Int,
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
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
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
