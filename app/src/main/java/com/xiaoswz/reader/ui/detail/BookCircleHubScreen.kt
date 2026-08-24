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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.xiaoswz.reader.data.api.HubBookDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.NumberFormat
import java.util.Locale

private val dirRoleColorMap = mapOf(
    "owner" to Color(0xFFE0A200),
    "council" to Color(0xFF9B6DFF),
    "elder" to Color(0xFF3A7BFF),
    "member" to GlassTokens.SecondaryLabel,
)

@Composable
fun BookCircleHubScreen(
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    val vm: BookCircleHubViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val hub = state.hub

    Scaffold(
        topBar = { AppTopBar(title = "书圈 · 资产仪表盘", onBack = onBack, showLogo = false) },
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
                if (hub != null) {
                    HubSummaryCard(hub.totalNetWorth, hub.joinedCircles, hub.investedBooks)
                } else if (state.error != null) {
                    Text(state.error ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                } else {
                    Text("正在加载你的书圈资产…", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
            }

            if (hub != null && hub.books.isEmpty()) {
                item {
                    Text(
                        "你还没有加入任何书圈。去书籍详情页点「书圈」即可加入并领取初始书币、投资持股。",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
            }

            if (hub != null) {
                item {
                    Text("持仓明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                }
                items(hub.books, key = { it.bookId }) { book ->
                    HubBookRow(book = book, onClick = { onBookClick(book.bookId) })
                }
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(state.toast) {
        state.toast?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }
}

@Composable
private fun HubSummaryCard(netWorth: Double, joined: Int, invested: Int) {
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("资产净值（书币 × 锚定价）", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            Spacer(Modifier.height(4.dp))
            Text(
                nf.format(netWorth.toLong()) + " 书币",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.Label,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricChip(Icons.Default.AccountBalance, "已加入", joined.toString(), GlassTokens.SystemBlue)
                MetricChip(Icons.Default.PieChart, "已投资", invested.toString(), Color(0xFF9B6DFF))
                MetricChip(Icons.Default.MonetizationOn, "净资产", nf.format(netWorth.toLong()), Color(0xFFE0A200))
            }
        }
    }
}

@Composable
private fun MetricChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
        Text(label, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
    }
}

@Composable
private fun HubBookRow(book: HubBookDto, onClick: () -> Unit) {
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }
    val roleColor = dirRoleColorMap[book.role] ?: GlassTokens.SecondaryLabel
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("《${book.bookId}》", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "锚定价 ${String.format("%.2f", book.anchorPrice)} · 国库 ${nf.format(book.treasury)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
                if (book.isChairman) {
                    RoleBadge("董事长", Color(0xFFE0A200))
                } else if (book.isDirector) {
                    RoleBadge(dirRoleLabel[book.role] ?: "董事", Color(0xFF9B6DFF))
                }
            }
            Spacer(Modifier.height(12.dp))
            // 持股 / 持仓数据条
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatBlock("持股", "%.2f%%".format(book.mySharePct), GlassTokens.SystemBlue)
                StatBlock("投资份额", nf.format(book.myInvested), Color(0xFF9B6DFF))
                StatBlock("余额/锁仓", "${nf.format(book.myBalance)}/${nf.format(book.myLocked)}", GlassTokens.Label)
                StatBlock("净值", nf.format(book.netValue.toLong()), Color(0xFFE0A200))
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
    }
}
