package com.xiaoswz.reader.ui.plugin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.annotation.AnnotationRepository
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.PluginSummary
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.plugin.PluginManifest
import com.xiaoswz.reader.data.plugin.PluginRepository
import com.xiaoswz.reader.data.plugin.PluginStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.xiaoswz.reader.ui.theme.MetaIcons

/**
 * 创意工坊（0.16.3 重设计）。
 *
 * 对齐 Obsidian（核心插件 vs 社区插件分离）/ VS Code（编辑器核心 vs Marketplace 扩展）的成熟范式：
 * - 高亮/书签已解耦为阅读器内置能力，不再是「插件」，不在此出现；
 * - 广场 = 干净的社区市场（仅用户发布的 published 插件）+ 发布入口；
 * - 我的 = 已导入/安装的插件管理（启用/禁用/卸载）+ 导入入口（URL/文件）；
 * - 教程 = 纯文档（怎么做插件、怎么导入、怎么发布），不再是插件卡片。
 *
 * 创作去中心化：不在 App 内做 DIY 模板生成器，改为开放「导入」（从文件/https 链接），
 * 用户在外部做好清单后导入本机；要分享则经广场「发布插件」提交审核。
 *
 * 隔离铁律：仅消费 APP 自身资源与冲浪阅读独立后端，不碰主站 novel-site。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPlazaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("广场", "我的", "教程")
    // 选中某个插件后进入其内部页
    var selectedPlugin by remember { mutableStateOf<PluginManifest?>(null) }
    // 导入/发布流程的模态（null | "install" | "publish"）
    var importMode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            importMode != null -> if (importMode == "publish") "发布插件" else "导入插件"
                            selectedPlugin != null -> selectedPlugin!!.name
                            else -> "创意工坊"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            importMode != null -> importMode = null
                            selectedPlugin != null -> selectedPlugin = null
                            else -> onBack()
                        }
                    }) {
                        Icon(MetaIcons.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (importMode != null) {
                ImportPublishContent(mode = importMode!!, onBack = { importMode = null })
            } else if (selectedPlugin != null) {
                PluginDetailContent(manifest = selectedPlugin!!)
            } else {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) },
                            icon = if (index == 0) {
                                { Icon(MetaIcons.Extension, contentDescription = null) }
                            } else null,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> PlazaTab(onOpenPlugin = { selectedPlugin = it }, onPublish = { importMode = "publish" })
                        1 -> MineTab(onOpenPlugin = { selectedPlugin = it }, onImport = { importMode = "install" })
                        2 -> TutorialTab()
                    }
                }
            }
        }
    }
}

// ───────────────────────── 广场（Discover） ─────────────────────────
@Composable
private fun PlazaTab(onOpenPlugin: (PluginManifest) -> Unit, onPublish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PlazaItem>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var installingId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }

    fun toItem(p: PluginSummary) =
        PlazaItem(p.pluginId, p.name, p.author, p.description, p.icon, p.type, p.installs, p.likes, p.pinned)

    fun load(reset: Boolean) {
        val nextPage = if (reset) 1 else page + 1
        loading = true
        scope.launch {
            runCatching { BackendClient.api.getPlugins(type = selectedType, page = nextPage) }
                .onSuccess { resp ->
                    items = if (reset) resp.items.map(::toItem) else items + resp.items.map(::toItem)
                    total = resp.total
                    page = nextPage
                }
                .onFailure { if (reset) items = emptyList() }
            loading = false
        }
    }

    // 修复（M16）：原先同时存在 LaunchedEffect(Unit) 和 LaunchedEffect(selectedType)，
    // 进入广场会并发发两次列表请求。selectedType 初值也是 null，保留它即可覆盖首屏加载。
    LaunchedEffect(selectedType) { load(true) }

    val typeTabs = listOf(
        null to "全部",
        "annotation" to "标注",
        "theme" to "主题",
        "decorator" to "装饰",
        "toolbar" to "工具",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 发布入口（干净的门户顶部，不与用户内容争 C 位）
        item {
            MetaButton(text = "+ 发布我的插件", onClick = onPublish, modifier = Modifier.fillMaxWidth())
        }

        // 分类筛选（横向滚动，避免占用纵向空间）
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                typeTabs.forEach { (t, label) ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                        label = { Text(label) },
                    )
                }
            }
        }

        item {
            Text("广场精选", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        if (loading && items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
        } else if (items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("广场还没有插件。你做好后可以点上方「发布我的插件」分享给大家~", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(items, key = { "plaza:${it.pluginId}" }) { item ->
                val manifest = PluginManifest(
                    id = item.pluginId,
                    name = item.name,
                    version = 1,
                    author = item.author,
                    description = item.description,
                    icon = item.icon,
                    minAppVersion = 67,
                    type = item.type,
                )
                PluginCard(
                    icon = item.icon.ifBlank { "🧩" },
                    name = item.name,
                    author = item.author,
                    description = item.description,
                    badge = if (item.pinned) "置顶" else "安装 ${item.installs}",
                    actionLabel = if (installingId == item.pluginId) "安装中…" else "安装",
                    enabled = installingId == null,
                    onAction = {
                        installingId = item.pluginId
                        scope.launch {
                            PluginRepository.installFromNetwork(context, item.pluginId)
                                .onSuccess {
                                    Toast.makeText(context, "已安装：${item.name}", Toast.LENGTH_SHORT).show()
                                    runCatching { BackendClient.api.installPlugin(item.pluginId) }
                                        .onSuccess { ack ->
                                            items = items.map { if (it.pluginId == item.pluginId) it.copy(installs = ack.installs) else it }
                                        }
                                }
                                .onFailure { e ->
                                    Toast.makeText(context, "安装失败：${e.message ?: "网络错误"}", Toast.LENGTH_SHORT).show()
                                }
                            installingId = null
                        }
                    },
                    likeLabel = "赞 ${item.likes}",
                    onLike = {
                        items = items.map { if (it.pluginId == item.pluginId) it.copy(likes = it.likes + 1) else it }
                        scope.launch {
                            runCatching { BackendClient.api.likePlugin(item.pluginId) }
                                .onSuccess { ack ->
                                    items = items.map { if (it.pluginId == item.pluginId) it.copy(likes = ack.likes) else it }
                                }
                        }
                    },
                    onClick = { onOpenPlugin(manifest) },
                )
            }
            // 分页：仍有更多时展示「加载更多」
            if (items.size < total) {
                item {
                    MetaButton(
                        text = if (loading) "加载中…" else "加载更多",
                        onClick = { load(false) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                    )
                }
            }
        }
    }
}

private data class PlazaItem(
    val pluginId: String,
    val name: String,
    val author: String,
    val description: String,
    val icon: String,
    val type: String,
    val installs: Int,
    val likes: Int,
    val pinned: Boolean,
)

@Composable
private fun PluginCard(
    icon: String,
    name: String,
    author: String,
    description: String,
    badge: String,
    actionLabel: String?,
    enabled: Boolean = true,
    onAction: () -> Unit,
    onClick: () -> Unit = {},
    likeLabel: String? = null,
    onLike: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                fontSize = 30.sp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text("by $author", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (actionLabel != null) {
                Spacer(Modifier.width(8.dp))
                MetaButton(text = actionLabel, onClick = onAction, modifier = Modifier, variant = MetaButtonVariant.Cobalt, enabled = enabled)
            }
            if (likeLabel != null && onLike != null) {
                Spacer(Modifier.width(8.dp))
                MetaButton(text = likeLabel, onClick = onLike, variant = MetaButtonVariant.Ghost)
            }
        }
    }
}

// ───────────────────────── 我的（Installed / Manage） ─────────────────────────
@Composable
private fun MineTab(onOpenPlugin: (PluginManifest) -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed by PluginStateStore.installedFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    // 「我的发布」：本机记录提交过的插件 id，向后台拉取审核状态（pending/published/rejected）。
    val submittedIds by PluginStateStore.submittedFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    var submittedItems by remember { mutableStateOf<List<PluginSummary>>(emptyList()) }
    var loadingSubmitted by remember { mutableStateOf(false) }
    LaunchedEffect(submittedIds) {
        if (submittedIds.isEmpty()) { submittedItems = emptyList(); return@LaunchedEffect }
        loadingSubmitted = true
        runCatching { BackendClient.api.getPluginsStatus(submittedIds.joinToString(",")) }
            .onSuccess { submittedItems = it.items }
            .onFailure { submittedItems = emptyList() }
        loadingSubmitted = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 「我的发布」：提交后在广场的审核状态（UGC 闭环的用户侧反馈）
        item {
            Text("我提交的插件（审核状态）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        if (submittedIds.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("你还没有提交过插件。去广场点「发布我的插件」分享你的作品~", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (loadingSubmitted) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp) } }
        } else {
            items(submittedItems, key = { "submitted:${it.pluginId}" }) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.icon.ifBlank { "🧩" }, fontSize = 28.sp, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("by ${s.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val (badge, badgeColor) = when (s.status) {
                            "published" -> "已通过" to MaterialTheme.colorScheme.primary
                            "pending" -> "待审核" to MaterialTheme.colorScheme.onSurfaceVariant
                            "rejected" -> "已拒绝" to MaterialTheme.colorScheme.error
                            else -> s.status to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor)
                    }
                }
            }
        }

        // 导入入口：对应 Obsidian 手动放入插件目录 / VS Code 从 VSIX 安装
        item {
            MetaButton(text = "+ 导入插件（文件 / https 链接）", onClick = onImport, modifier = Modifier.fillMaxWidth())
        }

        item {
            Text("已安装（导入 / 广场）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        if (installed.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("还没有安装任何插件。去广场看看，或点上方「导入插件」把外部做好的清单装进来~", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(installed, key = { "installed:${it.manifest.id}" }) { install ->
                val m = install.manifest
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPlugin(m) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(m.icon.ifBlank { "🧩" }, fontSize = 28.sp, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("by ${m.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (install.enabled) "已启用" else "已停用", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Switch(checked = install.enabled, onCheckedChange = { on ->
                                scope.launch { PluginRepository.setEnabled(context, m.id, on) }
                            })
                            Spacer(Modifier.width(8.dp))
                            MetaButton(text = "卸载", onClick = { scope.launch { PluginRepository.uninstall(context, m.id) } })
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 导入 / 发布（去中心化创作入口） ─────────────────────────
@Composable
private fun ImportPublishContent(mode: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var json by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<PluginManifest?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { text ->
            if (text != null) { json = text; preview = null; error = null }
        }.onFailure { error = "读取文件失败：${it.message}" }
    }

    fun tryPreview() {
        preview = PluginManifest.fromJson(json)
        error = if (preview == null) "清单解析失败：需为合法 JSON 且含 id / name / type" else null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (mode == "publish") "发布插件到广场" else "导入插件到本机",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (mode == "publish") {
                "把你在外部做好的清单提交到广场，经官方审核（pending → published）后上架，供所有读者安装。提交即进入待审核。"
            } else {
                "把外部做好的清单导入本机立即生效（只本机，不发布）。可粘贴 JSON、从文件选择、或填 https 链接（如 GitHub raw）。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("清单 JSON", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = json,
            onValueChange = { json = it; preview = null },
            label = { Text("粘贴 manifest JSON") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            maxLines = 12,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaButton(text = "从文件选择", onClick = { docLauncher.launch(arrayOf("application/json", "text/plain")) })
            MetaButton(text = "预览", onClick = { tryPreview() })
        }

        Text("或从 https 链接导入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("https 链接（如 GitHub raw）") },
                modifier = Modifier.weight(1f),
            )
            MetaButton(text = "读取", onClick = {
                if (!url.startsWith("https://")) { error = "仅支持 https 链接（明文 http 已被系统拦截）"; return@MetaButton }
                busy = true; error = null
                scope.launch {
                    PluginRepository.importFromUrl(context, url)
                        .onSuccess { m -> json = ""; preview = m; Toast.makeText(context, "已读取清单：${m.name}", Toast.LENGTH_SHORT).show() }
                        .onFailure { e -> error = "读取失败：${e.message}" }
                    busy = false
                }
            }, enabled = !busy)
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        preview?.let { m ->
            Spacer(Modifier.height(8.dp))
            Text("预览：${m.name}（${m.type}） by ${m.author}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(Modifier.fillMaxWidth()) {
                Text(json.ifBlank { "（已从链接读取）" }, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MetaButton(
                text = if (busy) "处理中…" else if (mode == "publish") "提交到广场" else "安装到本机",
                onClick = {
                    busy = true
                    scope.launch {
                        if (mode == "publish") {
                            runCatching { BackendClient.api.submitPlugin(m) }
                                .onSuccess { ack ->
                                    // 记录本次提交，供「我的发布」查询审核状态（本机维度）。
                                    PluginStateStore.addSubmitted(context, m.id)
                                    Toast.makeText(context, if (ack.status == "published") "已发布到广场" else "已提交，待审核", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .onFailure { e -> error = "提交失败：${e.message}"; busy = false }
                        } else {
                            PluginRepository.installLocal(context, m)
                            Toast.makeText(context, "已导入到本机：${m.name}", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ───────────────────────── 教程（纯文档，不再作为插件卡片） ─────────────────────────
@Composable
private fun TutorialTab() {
    val tutorial = """
        冲浪阅读 · 插件制作教程

        一、插件是什么
        插件是一份 JSON 清单（纯数据，不是代码），由 APP 内置的「能力槽」解释执行。
        它不会加载任何动态代码，所以永远不会拖垮或劫持你的阅读体验。

        二、你能做什么插件
        · 选区动作（annotation）：选中文字后在选区菜单追加你的动作（划线 / 摘抄 / 加书签等），
          点击即写入标注，登录后跨设备同步。
        · 主题 / 工具栏 / 侧栏弹层等能力槽已开放（安装对应类型插件即生效）。

        三、怎么做（去中心化，不在 App 内拖拽生成）
        1. 从下方「最小可用清单」复制一份，本地改 JSON 即可，零环境依赖。
        2. 在 App 内「我的 → 导入插件」粘贴 JSON、选文件、或填 https 链接（如 GitHub raw）即可安装到本机。
        3. 想分享给别人：在「广场 → 发布插件」粘贴清单提交，经官方审核后上架，所有人都能装。

        四、最小可用清单
        {
          "id": "local.myhl",
          "name": "我的划线",
          "type": "annotation",
          "capabilities": {
            "annotation": {
              "annotationType": "highlight",
              "label": "划线",
              "defaultColor": -14336,
              "withNote": false
            }
          }
        }

        五、字段说明
        · id：反向域名，全局唯一；本地建议 local. 开头。
        · type：决定主能力槽，当前支持 annotation（后续 theme / toolbar / open_sheet / decorator）。
        · annotation.annotationType：写入标注的类型（开放字符串）。
        · annotation.label：选区菜单显示名。
        · annotation.defaultColor：ARGB 颜色整数（如 -14336 为黄色）。
        · annotation.withNote：是否允许顺手写备注。

        六、隔离说明
        创意工坊只围绕 APP 自身资源（标注 / 主题 / 工具栏 / 选区）扩展，
        与站点主程序完全独立，互不耦合。
    """.trimIndent()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("插件制作教程", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(tutorial, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ════════════════════════════════════════════════════════════════
//  插件内部页：头部含启用/停用开关，正文按类型呈现：标注类 → 收藏段落合集；
//  其余 → 清单 JSON。
// ════════════════════════════════════════════════════════════════

/** 一条标注的展示模型（含解析出的书名） */
private data class AnnoDisplay(
    val bookId: String,
    val bookTitle: String,
    val chapterId: String,
    val entity: AnnotationEntity,
)

@Composable
private fun PluginDetailContent(manifest: PluginManifest) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed by PluginStateStore.installedFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val install = installed.find { it.manifest.id == manifest.id }
    val isEnabled = install?.enabled ?: false

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(manifest.icon.ifBlank { "🧩" }, fontSize = 40.sp, modifier = Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(manifest.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("by ${manifest.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(manifest.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isEnabled) "已启用" else "已停用", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Switch(checked = isEnabled, onCheckedChange = { on ->
                    scope.launch { PluginRepository.setEnabled(context, manifest.id, on) }
                })
                Spacer(Modifier.weight(1f))
                MetaButton(text = "卸载", onClick = { scope.launch { PluginRepository.uninstall(context, manifest.id) } }, modifier = Modifier.weight(1f))
            }
        }

        when (manifest.type) {
            "annotation" -> AnnotationCollectionContent(context, manifest, scope)
            else -> ManifestJsonContent(manifest)
        }
    }
}

/** 标注类插件正文：展示全部「该类型」的收藏段落合集（书名 + 章 + 原文 + 删除）。 */
@Composable
private fun AnnotationCollectionContent(
    context: android.content.Context,
    manifest: PluginManifest,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val annotationType = manifest.capabilities.annotation?.annotationType
    val items = remember { mutableStateListOf<AnnoDisplay>() }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            // 修复（M17）：遍历 annotations/ 目录全部 JSON 属重 IO，原先在主线程执行；
            // 整段切到 Dispatchers.IO，避免打开标注类插件时掉帧。
            val disp = withContext(Dispatchers.IO) {
                val shelf = BookshelfRepository(context.applicationContext)
                val all = AnnotationRepository.loadAll(context)
                    .filter { annotationType == null || it.entity.type == annotationType }
                all.mapNotNull { a ->
                    val book = runCatching { shelf.getBySlug(a.bookId) }.getOrNull()
                    AnnoDisplay(a.bookId, book?.title ?: a.bookId, a.entity.chapterId, a.entity)
                }.sortedByDescending { it.entity.updatedAt }
            }
            items.clear()
            items.addAll(disp)
        }
        loaded = true
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "已收藏的「${manifest.name}」段落",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        if (loaded && items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "还没有任何记录。去阅读时选中文字 → 点底部「✎」→ 选「${manifest.capabilities.annotation?.label ?: "收藏"}」即可加入。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.entity.clientId }) { d ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    d.bookTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            AnnotationRepository.deleteOne(context, d.bookId, d.entity.clientId)
                                            items.remove(d)
                                        }
                                    },
                                ) { Icon(MetaIcons.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                            }
                            Text(
                                "第 ${d.chapterId} 章",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                d.entity.quotedText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                formatTime(d.entity.updatedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 其余类型插件正文：直接展示清单 JSON */
@Composable
private fun ManifestJsonContent(manifest: PluginManifest) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("清单（JSON）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        val json = remember(manifest) {
            buildString {
                append("{\n")
                append("  \"id\": \"${manifest.id}\",\n")
                append("  \"name\": \"${manifest.name}\",\n")
                append("  \"type\": \"${manifest.type}\",\n")
                append("  \"author\": \"${manifest.author}\"\n")
                append("}")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(json, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(ts: Long): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
        sdf.format(java.util.Date(ts))
    } catch (_: Throwable) { "" }
}
