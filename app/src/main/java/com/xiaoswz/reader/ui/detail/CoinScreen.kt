package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.xiaoswz.reader.data.api.CoinLedgerDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ledgerLabel = mapOf(
    "earn_read" to "阅读奖励",
    "earn_comment" to "章评奖励",
    "earn_tag" to "标签贡献",
    "earn_heart" to "比心奖励",
    "spend_bid" to "竞拍圈主",
    "spend_invest" to "投资书籍",
    "dividend" to "投资分红",
    "admin_grant" to "管理员发放",
    "penalty" to "处罚扣除",
)

@Composable
fun CoinScreen(onBack: () -> Unit) {
    val vm: CoinViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 触底加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIdx ->
                if (lastIdx != null && lastIdx >= state.ledger.size - 2) vm.loadMore()
            }
    }

    Scaffold(
        topBar = { AppTopBar(title = "书币账本", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFE0A200), modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("书币总资产", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel)
                            Spacer(Modifier.height(2.dp))
                            Text("${state.balance}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                            Spacer(Modifier.height(2.dp))
                            Text("累计获得 ${state.earnedTotal} · 锁仓 ${state.locked}（跨 ${state.coins.size} 种书币）", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("每一笔书币进出都公开可查——透明本身就是激励。书币为固定池硬通货，每书独立铸造，互不相同。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }

            // ── 0.18 按书持仓明细 ──
            item {
                Text("各书币持仓", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            if (state.coins.isEmpty()) {
                item {
                    Text("暂无持仓。去书籍书圈领取初始书币或投资即可获得本书币。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
            } else {
                items(state.coins, key = { it.bookId }) { coin ->
                    Card(
                        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("《${coin.bookId}》", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                                Text("锁仓 ${coin.locked} · 累计获得 ${coin.earnedTotal}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                            }
                            Text("${coin.balance}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE0A200))
                        }
                    }
                }
            }

            if (state.error != null) {
                item { Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828)) }
            }

            if (state.ledger.isEmpty() && state.error == null) {
                item { Text("还没有书币流水，去阅读或参与书圈即可获得。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel) }
            }

            items(state.ledger, key = { it.id }) { item ->
                LedgerRow(item = item)
            }
        }
    }
}

@Composable
private fun LedgerRow(item: CoinLedgerDto) {
    val positive = item.delta >= 0
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ledgerLabel[item.type] ?: item.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTokens.Label,
                )
                item.reason?.let { r ->
                    Text(r, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
                Text(fmtTs(item.createdAt), style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }
            Text(
                "${if (positive) "+" else ""}${item.delta}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) Color(0xFF2E7D32) else Color(0xFFC62828),
            )
        }
    }
}

private fun fmtTs(ts: Long): String {
    if (ts <= 0) return ""
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}
