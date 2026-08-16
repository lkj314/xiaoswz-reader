package com.xiaoswz.reader.ui.plugin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
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
import androidx.compose.material3.TopAppBar
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
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.plugin.BundledPlugins
import com.xiaoswz.reader.data.plugin.PluginManifest
import com.xiaoswz.reader.data.plugin.PluginRepository
import com.xiaoswz.reader.data.plugin.PluginStateStore
import kotlinx.coroutines.launch

/**
 * 创意工坊 · 插件广场（0.12.0 v1）。
 *
 * 内部四 Tab：
 * - 广场：从冲浪阅读自有后端拉取插件列表（当前 mock，DB 批准后接真实 Plugin 表），可一键安装；
 *         官方内置插件在此标「已内置」。
 * - 我的：管理已装 / 内置插件（停用 / 卸载 DIY 插件）。
 * - 制作：可视化生成一个本地 DIY 插件（只本机生效，不可发布），无需写代码。
 * - 教程：内置《插件制作教程》要点 + 示例清单（与本地 .md 教程同源，不入库）。
 *
 * 严格遵守隔离铁律：仅消费 APP 自身资源与冲浪阅读独立后端，不碰主站 novel-site。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginPlazaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("广场", "我的", "制作", "教程")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创意工坊", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                        icon = if (index == 0) {
                            { Icon(Icons.Default.Extension, contentDescription = null) }
                        } else null,
                    )
                }
            }
            when (selectedTab) {
                0 -> PlazaTab(onBack)
                1 -> MineTab()
                2 -> MakeTab()
                3 -> TutorialTab()
            }
        }
    }
}

// ───────────────────────── 广场 ─────────────────────────
@Composable
private fun PlazaTab(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PlazaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var installingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { BackendClient.api.getPlugins(pinned = true) }
            .onSuccess { resp ->
                items = resp.items.map {
                    PlazaItem(it.pluginId, it.name, it.author, it.description, it.icon, it.installs, it.likes, it.pinned)
                }
            }
            .onFailure {
                // mock 接口失败不影响内置插件展示
            }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 官方内置插件（离线可用，已生效）
        item {
            Text("官方内置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        items(BundledPlugins.all, key = { it.id }) { manifest ->
            PluginCard(
                icon = manifest.icon,
                name = manifest.name,
                author = manifest.author,
                description = manifest.description,
                badge = "已内置",
                actionLabel = null,
                onAction = {},
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("广场精选", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
        } else if (items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("暂无广场插件，先试试官方内置~", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(items, key = { it.pluginId }) { item ->
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
                                }
                                .onFailure { e ->
                                    Toast.makeText(context, "安装失败：${e.message ?: "网络错误"}", Toast.LENGTH_SHORT).show()
                                }
                            installingId = null
                        }
                    },
                )
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Button(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}

// ───────────────────────── 我的 ─────────────────────────
@Composable
private fun MineTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed by PluginStateStore.installedFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("内置插件（不可卸载）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        items(BundledPlugins.all, key = { it.id }) { manifest ->
            PluginCard(
                icon = manifest.icon,
                name = manifest.name,
                author = manifest.author,
                description = manifest.description,
                badge = "内置",
                actionLabel = null,
                onAction = {},
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text("已安装（DIY / 广场）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        if (installed.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("还没有安装任何插件，去广场看看吧~", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(installed, key = { it.manifest.id }) { install ->
                val m = install.manifest
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Button(onClick = { scope.launch { PluginRepository.uninstall(context, m.id) } }) { Text("卸载") }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 制作 ─────────────────────────
@Composable
private fun MakeTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf("") }

    fun slugOf(input: String): String {
        val base = input.trim().replace(Regex("\\s+"), "_").lowercase()
        return if (base.isBlank()) "diy" else "local.$base"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("做一个本地 DIY 插件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "无需写代码：填好下面两项，就能生成一个「划词动作」插件，只在本机生效，不能发布到广场。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("插件名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("选区菜单显示名（如「划线」「摘抄」）") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("划线时允许顺手写备注", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Switch(checked = note, onCheckedChange = { note = it })
        }

        val type = "highlight"
        Button(
            onClick = {
                if (name.isBlank() || label.isBlank()) {
                    Toast.makeText(context, "名称和显示名不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                saving = true
                scope.launch {
                    val manifest = PluginManifest(
                        id = slugOf(name),
                        name = name.trim(),
                        version = 1,
                        author = "本机用户",
                        description = "本地 DIY 插件",
                        icon = "🛠️",
                        minAppVersion = 67,
                        type = "annotation",
                        capabilities = com.xiaoswz.reader.data.plugin.Capabilities(
                            annotation = com.xiaoswz.reader.data.plugin.AnnotationCap(
                                annotationType = type,
                                label = label.trim(),
                                defaultColor = -14336,
                                withNote = note,
                            ),
                        ),
                    )
                    PluginRepository.installLocal(context, manifest)
                    Toast.makeText(context, "已保存到本机：${manifest.name}", Toast.LENGTH_SHORT).show()
                    saving = false
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "保存中…" else "保存到本机") }

        Spacer(Modifier.height(8.dp))
        Text("预览清单（JSON）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        preview = buildString {
            append("{\n")
            append("  \"id\": \"${slugOf(name).ifBlank { "local.diy" }}\",\n")
            append("  \"name\": \"${name.ifBlank { "我的插件" }}\",\n")
            append("  \"type\": \"annotation\",\n")
            append("  \"capabilities\": {\n")
            append("    \"annotation\": {\n")
            append("      \"annotationType\": \"$type\",\n")
            append("      \"label\": \"${label.ifBlank { "划线" }}\",\n")
            append("      \"defaultColor\": -14336,\n")
            append("      \"withNote\": $note\n")
            append("    }\n")
            append("  }\n")
            append("}")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                preview,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ───────────────────────── 教程 ─────────────────────────
@Composable
private fun TutorialTab() {
    val tutorial = """
        冲浪阅读 · 插件制作教程（要点）

        一、什么是插件
        插件是一份 JSON 清单（数据，不是代码），由 APP 内置的「能力槽」解释执行。
        它不会加载任何动态代码，所以永远不会拖垮或劫持你的阅读体验。

        二、你能做什么插件
        · 选区动作（annotation）：选中一段文字后，在系统选区菜单里追加你的动作，
          比如「划线」「摘抄」「加书签」。点击即写入标注，登录后跨设备同步。
        · 主题 / 工具栏 / 渲染等能力槽将在后续版本开放。

        三、最小可用清单
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

        四、字段说明
        · id：反向域名，全局唯一；本地 DIY 建议以 local. 开头。
        · type：决定主能力槽，当前支持 annotation。
        · annotation.annotationType：写入标注的类型（开放字符串）。
        · annotation.label：选区菜单里显示的名字。
        · annotation.defaultColor：ARGB 颜色整数（如 -14336 为黄色）。
        · annotation.withNote：是否允许顺手写备注。

        五、发布到广场
        当前广场数据为示例；后端 Plugin 表就绪后，你将可以在 App 内提交插件，
        经官方审核（pending → published）后上架，供所有读者安装。

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
