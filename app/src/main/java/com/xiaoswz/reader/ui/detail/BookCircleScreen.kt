package com.xiaoswz.reader.ui.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import com.xiaoswz.reader.data.api.CircleRankItem
import com.xiaoswz.reader.data.api.ShareholderDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val roleLabel = mapOf(
    "owner" to "圈主",
    "council" to "议事员",
    "elder" to "长老",
    "member" to "书迷",
)
private val roleColor = mapOf(
    "owner" to Color(0xFFE0A200),
    "council" to Color(0xFF9B6DFF),
    "elder" to GlassTokens.SystemBlue,
    "member" to GlassTokens.SecondaryLabel,
)

@Composable
fun BookCircleScreen(
    bookId: String,
    bookTitle: String?,
    onBack: () -> Unit,
    onExchangeClick: (String) -> Unit = {},
    onBoardClick: (String) -> Unit = {},
) {
    val vm: BookCircleViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    LaunchedEffect(bookId) { vm.init(bookId, bookTitle) }

    Scaffold(
        topBar = { AppTopBar(title = "书圈", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                if (!bookTitle.isNullOrBlank()) {
                    Text(
                        bookTitle!!,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.Label,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // 我的本书币余额胶囊
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFE0A200), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "我的《${bookId}》书币：${state.balance}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlassTokens.Label,
                        fontWeight = FontWeight.Medium,
                    )
                    if (state.locked > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· 锁仓 ${state.locked}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "书币是固定池硬通货：每书圈一次性铸造 10 万枚，绝不增发、系统绝不付息。它不参与内容消费（阅读免费），只用于投资、持股治理、交易所互换与收藏展示——价值由市场共识决定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTokens.SecondaryLabel,
                )
            }

            // ── 初始领取（硬通货唯一入口）──
            item {
                val c = state.circle
                val myClaimed = c?.myMembership?.claimedAmount ?: 0
                if (c?.claimWindowOpen == true && myClaimed < 100) {
                    CircleGlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFE0A200), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("初始书币领取", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "书币为固定池硬通货，每书圈一次性铸造 10 万枚，发完即关闭领取通道。在本书做出过有效贡献（章评通过审核 / 提交角色标签）即可从国库领取，单人上限 100 枚。国库剩余 ${c.treasury} / 铸造 ${c.mintedTotal + c.treasury}。领完后不再增发。",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.SecondaryLabel,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("你已领取：$myClaimed / 100", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = vm::claim) { Text("领取初始书币") }
                        }
                    }
                }
            }

            // ── 董事会 / 圈主席位（持股治理，无竞拍）──
            item {
                val c = state.circle
                val isOwner = c?.ownerUserId != null && c.ownerUserId == c.myMembership?.userId
                CircleGlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("董事会 / 圈主席位", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "圈主（董事长）由投资份额最高者自动担任，无需竞拍——竞拍会令书币回流系统锁仓、切断流通。董事（长老 / 议事员）按持股占比自动晋升。董事会可调控基准币价、回购锁仓、调拨储备、发布财报。",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "现任圈主：${c?.ownerDisplayName ?: "（暂无）"}" + (if (isOwner) " · 就是你 🎉" else ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTokens.Label,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "基准书币价：${String.format("%.2f", c?.policyAnchorPrice ?: 1f)} · 流通量 ${c?.circulatingSupply ?: 0} · 股权总份额 ${c?.shareTotal ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { onBoardClick(bookId) }, modifier = Modifier.weight(1f)) { Text("进入董事会") }
                            OutlinedButtonCompat(onClick = { onExchangeClick(bookId) }, modifier = Modifier.weight(1f)) { Text("书币交易所") }
                        }
                    }
                }
            }

            // ── 股权结构 ──
            item {
                val c = state.circle
                val myUid = c?.myMembership?.userId
                CircleGlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFE0A200), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("股权结构（股东名册 Top10）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                        }
                        Spacer(Modifier.height(10.dp))
                        if (c?.shareholders.isNullOrEmpty()) {
                            Text("暂无股东。投资本书即可成为股东、获取圈主资格。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        } else {
                            c?.shareholders?.forEachIndexed { idx, sh ->
                                ShareholderRow(sh = sh, isMe = sh.userId == myUid, rank = idx + 1)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // ── 储备（持有他书币）──
            item {
                val reserves = state.circle?.reserves?.filter { it.assetBookId != bookId && it.amount > 0 }
                if (!reserves.isNullOrEmpty()) {
                    CircleGlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("书圈储备（持有他书币）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                            Spacer(Modifier.height(8.dp))
                            reserves.forEach { r ->
                                Text("《${r.assetBookId}》：${r.amount} 枚", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // ── 加入书圈 / 每书人设 ──
            item {
                val c = state.circle
                if (c?.myMembership == null) {
                    CircleGlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("加入书圈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "加入后可设定本书圈专属昵称（如「唐门弟子」），增强沉浸与归属感。",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.SecondaryLabel,
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = vm::openIdentityDialog) { Text("加入并设定人设") }
                        }
                    }
                } else {
                    CircleGlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFF9B6DFF), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("本书圈人设", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "昵称：${c.myMembership.displayName ?: "（继承主身份）"} · 声望 ${c.myMembership.reputation} · ${dirRoleLabel[c.myMembership.role] ?: "书迷"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            }
                            TextButton(onClick = vm::openIdentityDialog) { Text("修改", color = GlassTokens.SystemBlue) }
                        }
                    }
                }
            }

            // ── 投资 ──
            item {
                val c = state.circle
                CircleGlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFE0A200), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("投资这本书", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "用固定池「书币」投资本书获得份额——这是你与这本书的经济纽带，也是圈主资格的来源。锁定期内质押不可撤（防短线炒作）。投资回报严格只来自其他读者的 P2P 转账与份额二手交易，系统绝不付息：你越早支持，后期读者进场接盘时你的份额越值钱。",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("全书总投资：${c?.totalInvestment ?: 0} 书币 · 成长指数 ${(c?.growthIndex ?: 0f).let { "%.2f".format(it) }}", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label)
                        c?.myInvestment?.let { inv ->
                            Spacer(Modifier.height(8.dp))
                            val unlocked = inv.unlockAt <= System.currentTimeMillis()
                            Text(
                                "我的投资：份额 ${"%.2f".format(inv.sharePct)}% · 质押 ${inv.amount} 书币 · 解锁 ${fmtDate(inv.unlockAt)}${if (unlocked) "（可撤回）" else "（锁仓中）"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.SystemBlue,
                                fontWeight = FontWeight.Medium,
                            )
                            if (unlocked && inv.status == "active") {
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = vm::withdraw) { Text("撤回投资（质押退回）") }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (c?.canInvest == true) {
                            Button(onClick = vm::openInvestDialog) { Text("投资本书") }
                        } else {
                            Text("你已加入且暂无投资资格，或已达单书上限", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        }
                    }
                }
            }

            // ── 排行 ──
            item {
                Text("书圈声望排行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            if (state.rank.isEmpty()) {
                item {
                    Text("暂无排行数据", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
            } else {
                items(state.rank, key = { it.userId }) { item ->
                    RankRow(item = item)
                }
            }
        }
    }

    // ── 对话框 ──
    if (state.showInvestDialog) {
        NumberDialog(
            title = "投资本书",
            hint = "投入书币（上限 ${state.circle?.investMaxPerBook ?: 5000}）",
            value = state.investAmount,
            onValue = vm::setInvestAmount,
            onConfirm = vm::confirmInvest,
            onDismiss = vm::dismissInvestDialog,
        )
    }
    if (state.showIdentityDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissIdentityDialog,
            title = { Text("设定本书圈人设") },
            text = {
                Column {
                    Text("为本书圈设定一个专属昵称（留空则继承主身份）。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.identityName,
                        onValueChange = vm::setIdentityName,
                        label = { Text("本书圈昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { Button(onClick = { vm.joinWithIdentity(state.identityName) }) { Text("加入 / 保存") } },
            dismissButton = { TextButton(onClick = vm::dismissIdentityDialog) { Text("取消") } },
        )
    }
    if (state.showFeatureDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissFeatureDialog,
            title = { Text("圈主精选") },
            text = {
                Column {
                    Text("输入要精选的评论 ID（章评/书评），被精选的评论将获「圈主精选」标记。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.featureCommentId,
                        onValueChange = vm::setFeatureCommentId,
                        label = { Text("评论 ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { Button(onClick = vm::confirmFeature) { Text("精选") } },
            dismissButton = { TextButton(onClick = vm::dismissFeatureDialog) { Text("取消") } },
        )
    }

    if (state.showTransferDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissTransferDialog,
            title = { Text("转账 / 打赏（P2P）") },
            text = {
                Column {
                    Text("书币在用户之间直接流转，系统零抽成。填入收款人 ID（可在书圈排行或其他读者处获取）与金额。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.transferTarget,
                        onValueChange = vm::setTransferTarget,
                        label = { Text("收款人 ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.transferAmount,
                        onValueChange = vm::setTransferAmount,
                        label = { Text("金额（余额 ${state.balance}）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { Button(onClick = vm::confirmTransfer) { Text("转账") } },
            dismissButton = { TextButton(onClick = vm::dismissTransferDialog) { Text("取消") } },
        )
    }

    // Toast（可见提示）
    val context = LocalContext.current
    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }
}

@Composable
private fun ShareholderRow(sh: ShareholderDto, isMe: Boolean, rank: Int) {
    val color = roleColor[sh.role] ?: GlassTokens.SecondaryLabel
    val barFrac = (sh.pct / 100f).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (isMe) Modifier.background(Color(0xFFE0A200).copy(alpha = 0.08f)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("#$rank", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(sh.displayName ?: "书迷", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GlassTokens.Label, modifier = Modifier.weight(1f))
                Text("${"%.2f".format(sh.pct)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(6.dp))
            // 占比条
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlassTokens.SecondaryLabel.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(barFrac).height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("份额 ${sh.shares} · ${dirRoleLabel[sh.role] ?: "书迷"}${if (isMe) " · 你" else ""}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
        }
    }
}

@Composable
private fun CircleGlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) { content() }
}

/** 次级按钮（描边），用于「书币交易所」等次要操作 */
@Composable
private fun OutlinedButtonCompat(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TextButton(onClick = onClick, modifier = modifier) { content() }
}

@Composable
private fun RankRow(item: CircleRankItem) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.EmojiEvents, null, tint = roleColor[item.role] ?: GlassTokens.SecondaryLabel, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.displayName ?: "书迷",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTokens.Label,
                )
                Text(
                    "声望 ${item.reputation} · 投资份额 ${item.investedShares}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            val label = roleLabel[item.role]
            if (label != null) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = roleColor[item.role] ?: GlassTokens.SecondaryLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NumberDialog(
    title: String,
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                label = { Text(hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun fmtDate(ts: Long): String {
    if (ts <= 0) return "—"
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))
    } catch (_: Exception) {
        "—"
    }
}
