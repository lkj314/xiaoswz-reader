package com.xiaoswz.reader.ui.plugin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.xiaoswz.reader.data.annotation.AnnotationEntity
import com.xiaoswz.reader.data.annotation.AnnotationRepository
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
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
    // 选中某个插件后进入其内部页（不再「嵌死」为死卡片）
    var selectedPlugin by remember { mutableStateOf<PluginManifest?>(null) }
    // 从插件详情「编辑」进入制作 Tab 时，携带待编辑的 DIY 清单
    var editingManifest by remember { mutableStateOf<PluginManifest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedPlugin?.name ?: "创意工坊", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPlugin != null) selectedPlugin = null else onBack()
                    }) {
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
            if (selectedPlugin != null) {
                // 插件内部页：头部（启用/停用）+ 正文（书签/划线集合 / 教程 / 清单）
                PluginDetailContent(manifest = selectedPlugin!!, onEdit = {
                    editingManifest = selectedPlugin
                    selectedPlugin = null
                    selectedTab = 2
                })
            } else {
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
            // 内容区用 Box(fillMaxSize) 框住：给内部 LazyColumn / 滚动容器一个有限、确定的高度，
            // 避免「滚动容器 fillMaxSize 直接挂在外层 Column」引发的约束成环 / 重测崩溃。
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                when (selectedTab) {
                    0 -> PlazaTab(onOpenPlugin = { selectedPlugin = it })
                    1 -> MineTab(onOpenPlugin = { selectedPlugin = it })
                    2 -> MakeTab(editing = editingManifest) { editingManifest = null }
                    3 -> TutorialTab()
                }
            }
            } // else
        }
    }
}

// ───────────────────────── 广场 ─────────────────────────
@Composable
private fun PlazaTab(onOpenPlugin: (PluginManifest) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PlazaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var installingId by remember { mutableStateOf<String?>(null) }
    // 内置插件 id 集合：广场里已内置的插件不再作为「安装」项重复出现，也避免 key 冲突。
    val bundledIds = remember { BundledPlugins.all.map { it.id }.toSet() }

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
        items(BundledPlugins.all.distinctBy { it.id }, key = { "bundled:${it.id}" }) { manifest ->
            PluginCard(
                icon = manifest.icon,
                name = manifest.name,
                author = manifest.author,
                description = manifest.description,
                badge = "已内置",
                actionLabel = null,
                onAction = {},
                onClick = { onOpenPlugin(manifest) },
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
            items(items.filter { it.pluginId !in bundledIds }, key = { "plaza:${it.pluginId}" }) { item ->
                // 用 plaza 拉来的清单构造临时 manifest，供点开详情 / 安装
                val manifest = PluginManifest(
                    id = item.pluginId,
                    name = item.name,
                    version = 1,
                    author = item.author,
                    description = item.description,
                    icon = item.icon,
                    minAppVersion = 67,
                    type = "annotation",
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
                                }
                                .onFailure { e ->
                                    Toast.makeText(context, "安装失败：${e.message ?: "网络错误"}", Toast.LENGTH_SHORT).show()
                                }
                            installingId = null
                        }
                    },
                    onClick = { onOpenPlugin(manifest) },
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
    onClick: () -> Unit = {},
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
                Button(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}

// ───────────────────────── 我的 ─────────────────────────
@Composable
private fun MineTab(onOpenPlugin: (PluginManifest) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed by PluginStateStore.installedFlow(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val disabledBundled by PluginStateStore.disabledBundledFlow(context).collectAsStateWithLifecycle(initialValue = emptySet())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("内置插件（不可卸载，可停用）", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }
        items(BundledPlugins.all.distinctBy { it.id }, key = { "bundled:${it.id}" }) { manifest ->
            val enabled = manifest.id !in disabledBundled
            PluginCard(
                icon = manifest.icon,
                name = manifest.name,
                author = manifest.author,
                description = manifest.description,
                badge = if (enabled) "内置" else "已停用",
                actionLabel = null,
                onAction = {},
                onClick = { onOpenPlugin(manifest) },
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
private fun MakeTab(editing: PluginManifest?, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(false) }
    var typePreset by remember { mutableStateOf("highlight") } // highlight / bookmark / custom
    var customType by remember { mutableStateOf("") }
    var color by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }

    // 进入编辑态：一次性回填待编辑 DIY 插件的字段
    LaunchedEffect(editing) {
        if (editing != null) {
            name = editing.name
            label = editing.capabilities.annotation?.label ?: ""
            note = editing.capabilities.annotation?.withNote ?: false
            val at = editing.capabilities.annotation?.annotationType ?: "highlight"
            if (at == "highlight" || at == "bookmark") { typePreset = at; customType = "" }
            else { typePreset = "custom"; customType = at }
            color = editing.capabilities.annotation?.defaultColor
        }
    }

    fun slugOf(input: String): String {
        val base = input.trim().replace(Regex("\\s+"), "_").lowercase()
        return if (base.isBlank()) "diy" else "local.$base"
    }
    val effectiveType = if (typePreset == "custom") customType.trim().ifBlank { "highlight" } else typePreset

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (editing != null) {
            Text("正在编辑：${editing.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            Text("做一个本地 DIY 插件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            "无需写代码：选好动作类型、挑个颜色、填好名称，就能生成一个「划词动作」插件。只在本机生效，不能发布到广场。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 动作类型
        Text("动作类型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipSelected("划线", typePreset == "highlight") { typePreset = "highlight" }
            FilterChipSelected("书签", typePreset == "bookmark") { typePreset = "bookmark" }
            FilterChipSelected("自定义", typePreset == "custom") { typePreset = "custom" }
        }
        if (typePreset == "custom") {
            OutlinedTextField(value = customType, onValueChange = { customType = it }, label = { Text("自定义类型标识（如 excerpt）") }, modifier = Modifier.fillMaxWidth())
        }

        // 颜色
        Text("颜色（书签默认无底色）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf<Pair<Int?, String>>(
                null to "无",
                (-14336) to "黄",
                (-65536) to "红",
                (-16711936) to "绿",
                (-16776961) to "蓝",
                (-7650020) to "紫",
            )
            for ((c, name2) in presets) {
                ColorChip(c, name2, color == c) { color = c }
            }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("插件名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("选区菜单显示名（如「划线」「摘抄」）") }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("划线时允许顺手写备注", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Switch(checked = note, onCheckedChange = { note = it })
        }

        Button(
            onClick = {
                if (name.isBlank() || label.isBlank()) {
                    Toast.makeText(context, "名称和显示名不能为空", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                saving = true
                scope.launch {
                    val manifest = PluginManifest(
                        id = editing?.id ?: slugOf(name),
                        name = name.trim(),
                        version = (editing?.version ?: 0) + 1,
                        author = "本机用户",
                        description = "本地 DIY 插件",
                        icon = "🛠️",
                        minAppVersion = 67,
                        type = "annotation",
                        capabilities = com.xiaoswz.reader.data.plugin.Capabilities(
                            annotation = com.xiaoswz.reader.data.plugin.AnnotationCap(
                                annotationType = effectiveType,
                                label = label.trim(),
                                defaultColor = color,
                                withNote = note,
                            ),
                        ),
                    )
                    PluginRepository.installLocal(context, manifest)
                    Toast.makeText(context, if (editing != null) "已更新：${manifest.name}" else "已保存到本机：${manifest.name}", Toast.LENGTH_SHORT).show()
                    saving = false
                    onDone()
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "保存中…" else if (editing != null) "更新本机插件" else "保存到本机") }

        Spacer(Modifier.height(8.dp))
        Text("预览清单（JSON）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        // 预览仅在输入变化时重算，绝不在组合期写 state（避免无限重组合 / 重测崩溃）。
        val preview = remember(name, label, note, typePreset, customType, color) {
            buildString {
                append("{\n")
                append("  \"id\": \"${(editing?.id ?: slugOf(name)).ifBlank { "local.diy" }}\",\n")
                append("  \"name\": \"${name.ifBlank { "我的插件" }}\",\n")
                append("  \"type\": \"annotation\",\n")
                append("  \"capabilities\": {\n")
                append("    \"annotation\": {\n")
                append("      \"annotationType\": \"$effectiveType\",\n")
                append("      \"label\": \"${label.ifBlank { "划线" }}\",\n")
                append("      \"defaultColor\": ${color ?: "null"},\n")
                append("      \"withNote\": $note\n")
                append("    }\n")
                append("  }\n")
                append("}")
            }
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

@Composable
private fun FilterChipSelected(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColorChip(color: Int?, name: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
                )
                .clickable { onClick() }
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    else Modifier
                ),
        ) {
            if (color == null) {
                Text("∅", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ════════════════════════════════════════════════════════════════
//  插件内部页（0.13.0）：点开插件不再「嵌死」为死卡片，
//  头部含启用/停用开关，正文按类型呈现：标注类 → 收藏段落合集；
//  doc 类 → 教程；其余 → 清单 JSON。
// ════════════════════════════════════════════════════════════════

/** 一条标注的展示模型（含解析出的书名） */
private data class AnnoDisplay(
    val bookId: String,
    val bookTitle: String,
    val chapterId: String,
    val entity: AnnotationEntity,
)

@Composable
private fun PluginDetailContent(
    manifest: PluginManifest,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val disabledBundled by PluginStateStore.disabledBundledFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val installed by PluginStateStore.installedFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val isBundled = BundledPlugins.all.any { it.id == manifest.id }
    val isEnabled = if (isBundled) manifest.id !in disabledBundled
    else (installed.find { it.manifest.id == manifest.id }?.enabled ?: false)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 头部
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
                if (!isBundled) {
                    Button(onClick = onEdit) { Text("编辑") }
                }
            }
        }

        // 正文
        when (manifest.type) {
            "annotation" -> AnnotationCollectionContent(context, manifest, scope)
            "doc" -> TutorialBody()
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
            val shelf = BookshelfRepository(context.applicationContext)
            val all = AnnotationRepository.loadAll(context)
                .filter { annotationType == null || it.entity.type == annotationType }
            val disp = all.mapNotNull { a ->
                val book = runCatching { shelf.getBySlug(a.bookId) }.getOrNull()
                AnnoDisplay(a.bookId, book?.title ?: a.bookId, a.entity.chapterId, a.entity)
            }.sortedByDescending { it.entity.updatedAt }
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
                                ) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
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

/** doc 类插件正文：复用教程文本 */
@Composable
private fun TutorialBody() {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("插件制作教程", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "不用写代码，会改 JSON 就能做插件。\n\n" +
                "· 选区动作（annotation）：选中文字后在选区条追加你的动作（划线 / 摘抄 / 加书签），点击即写入标注，登录后跨设备同步。\n" +
                "· 主题 / 工具栏 / 渲染等能力槽将在后续版本开放。\n\n" +
                "最小清单：{ \"id\": \"local.myhl\", \"type\": \"annotation\", \"capabilities\": { \"annotation\": { \"annotationType\": \"highlight\", \"label\": \"划线\", \"defaultColor\": -14336, \"withNote\": false } } }\n\n" +
                "隔离说明：创意工坊只围绕 APP 自身资源扩展，与站点主程序完全独立。",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 其余类型插件正文：直接展示清单 JSON（DIY 插件可在此查看/核对） */
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
