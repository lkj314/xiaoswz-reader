package com.xiaoswz.reader.ui.settings

import com.xiaoswz.reader.ui.components.MetaButton
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.BuildConfig
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.auth.AuthRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.sync.SyncRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.ArtImage
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.xiaoswz.reader.ui.theme.MetaIcons

/**
 * 用户中心（0.8.0）：从设置页抽出的专属账号区。
 * 整合：账户（登录/退出/管理台 SSO）+ 云同步 + 阅读成就入口。
 */
@Composable
fun UserCenterScreen(
    onBack: () -> Unit,
    onAccountClick: () -> Unit = {},
    onReadingStats: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }

    val isLoggedIn by appSettings.isLoggedInFlow.collectAsState(initial = false)
    val accountEmail by appSettings.accountEmailFlow.collectAsState(initial = null)
    val accountRole by appSettings.accountRoleFlow.collectAsState(initial = "guest")
    val roleLabel = when (accountRole) {
        "admin" -> "管理员"
        "user" -> "用户"
        else -> "游客"
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    val syncRepo = remember { SyncRepository(context.applicationContext) }
    var lastSyncAt by remember { mutableStateOf(0L) }
    var autoSync by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var backendUrl by remember { mutableStateOf(BuildConfig.BACKEND_BASE_URL) }

    LaunchedEffect(Unit) {
        lastSyncAt = appSettings.lastSyncAtFlow.first()
        autoSync = appSettings.autoSyncFlow.first()
        backendUrl = appSettings.backendBaseUrlFlow.first()
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        scope.launch { AuthRepository.logout() }
                    },
                ) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") } },
            title = { Text("退出登录") },
            text = { Text("退出后本机将回落为游客身份，书架与进度仍保留在本地；云同步需重新登录。") },
        )
    }

    Scaffold(
        topBar = { AppTopBar(title = "用户中心", onBack = onBack, showLogo = false) },
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
            // 个人资料（品牌头像 + 账号）
            Card(
                modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    ArtImage(
                        path = "character/avatar_circle.png",
                        contentDescription = "冲浪阅读",
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            if (isLoggedIn) (accountEmail ?: "读者") else "游客",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.Label,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (isLoggedIn) "身份：$roleLabel" else "登录后解锁评论与跨设备云同步",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
            }

            // 账户操作
            Card(
                modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("账号", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                    if (isLoggedIn) {
                        MetaButton(
                            text = "退出登录",
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (accountRole == "admin") {
                            var ssoBusy by remember { mutableStateOf(false) }
                            var ssoError by remember { mutableStateOf<String?>(null) }
                            MetaButton(
                                text = if (ssoBusy) "正在打开管理台…" else "管理后台",
                                onClick = {
                                    scope.launch {
                                        ssoBusy = true
                                        ssoError = null
                                        val base = appSettings.getBackendBaseUrl().removeSuffix("/")
                                        try {
                                            val resp = BackendClient.api.exchangeAdminSso()
                                            if (!resp.ok || resp.sso.isEmpty()) {
                                                ssoError = "管理台凭据无效，请重新登录"
                                                ssoBusy = false
                                                return@launch
                                            }
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("$base/api/admin/sso/consume?token=${resp.sso}"),
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (t: Throwable) {
                                            ssoError = "进管理台失败：${t.message ?: t.javaClass.simpleName}"
                                        }
                                        ssoBusy = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !ssoBusy,
                            )
                            ssoError?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        MetaButton(text = "登录 / 注册", onClick = onAccountClick, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 云同步
            Card(
                modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MetaIcons.Cloud, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("云同步", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                    }
                    Text("后端地址：$backendUrl", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    if (isLoggedIn) {
                        Text(
                            if (lastSyncAt == 0L) "上次同步：从未" else "上次同步：${formatSyncTime(lastSyncAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("启动时自动同步", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label)
                            Switch(
                                checked = autoSync,
                                onCheckedChange = { v -> autoSync = v; scope.launch { appSettings.setAutoSync(v) } },
                            )
                        }
                        MetaButton(
                            text = if (syncing) "同步中…" else "立即同步",
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
                                        res.exceptionOrNull()?.message ?: "同步失败：后端未启动或网络不可达"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !syncing,
                        )
                        syncMsg?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.contains("成功")) GlassTokens.SystemBlue else MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Text("登录账号后可开启云同步：书架与阅读进度跨设备备份，换机/重装后登录同一账号即可恢复。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        MetaButton(text = "去登录", onClick = onAccountClick, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 阅读成就
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .whaleGlassCard()
                    .clickable { onReadingStats() },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MetaIcons.EmojiEvents, null, tint = GlassTokens.Gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("阅读成就", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(MetaIcons.ArrowForward, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** 同步时间格式化 */
private fun formatSyncTime(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
    return sdf.format(java.util.Date(ts))
}
