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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.xiaoswz.reader.data.api.NewsItemDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoardScreen(
    bookId: String,
    onBack: () -> Unit,
) {
    val vm: BoardViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    LaunchedEffect(bookId) { vm.init(bookId) }

    Scaffold(
        topBar = { AppTopBar(title = "董事会 · 《$bookId》", onBack = onBack, showLogo = false) },
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
                val c = state.circle
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("董事会控制台", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "国库剩余 ${nf.format(c?.treasury ?: 0)} · 基准书币价 ${String.format("%.2f", c?.policyAnchorPrice ?: 1f)} · 流通 ${nf.format(c?.circulatingSupply ?: 0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
            }

            if (!state.isDirector) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🔒 董事专属", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "董事会操作仅向董事长与董事开放。提升你在本书圈的投资份额：当持股达到长老 / 议事员阈值（按占比自动晋升），即获董事席位与调控权。当前你的角色：${dirRoleLabel[state.circle?.myMembership?.role] ?: "书迷"}。",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.SecondaryLabel,
                            )
                        }
                    }
                }
            } else {
                // 设定基准价
                item {
                    PanelCard("① 设定基准书币价（信号，非增发）") {
                        Text("董事会锚定本书币的参考价，影响展示与撮合区间，不改供给。市场成交最终决定真实币价。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.anchorPrice,
                            onValueChange = vm::setAnchorPrice,
                            label = { Text("基准价") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        MetaButton(text = "设定基准价", onClick = vm::setAnchor, modifier = Modifier.fillMaxWidth())
                    }
                }
                // 回购
                item {
                    PanelCard("② 国库回购（锁入储备，减少流通浮筹）") {
                        Text("用本书圈国库（未发放铸造）回购书币锁入储备，守恒：国库↓ = 自我储备↑。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.buybackAmount,
                            onValueChange = vm::setBuybackAmount,
                            label = { Text("回购数量") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        MetaButton(text = "执行回购", onClick = vm::buyback, modifier = Modifier.fillMaxWidth())
                    }
                }
                // 储备调拨
                item {
                    PanelCard("③ 储备调拨（用国库换入他书币）") {
                        Text("用本书圈国库换入其他书圈的书币作为储备，两池守恒。例如换入第一本书的币以巩固资产。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.reserveAssetBookId,
                            onValueChange = vm::setReserveAssetBookId,
                            label = { Text("目标书 ID（如 B000002）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.reserveAmount,
                            onValueChange = vm::setReserveAmount,
                            label = { Text("换入数量") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        MetaButton(text = "调拨储备", onClick = vm::moveReserve, modifier = Modifier.fillMaxWidth())
                    }
                }
                // 发布新闻稿
                item {
                    PanelCard("④ 发布财报 / 新闻稿（利好 · 利空 · 中性）") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SentimentChip("利好", "bull", state.newsSentiment == "bull", Color(0xFF2E7D32), vm::setNewsSentiment)
                            SentimentChip("中性", "neutral", state.newsSentiment == "neutral", GlassTokens.SecondaryLabel, vm::setNewsSentiment)
                            SentimentChip("利空", "bear", state.newsSentiment == "bear", Color(0xFFC62828), vm::setNewsSentiment)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.newsTitle,
                            onValueChange = vm::setNewsTitle,
                            label = { Text("标题") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.newsBody,
                            onValueChange = vm::setNewsBody,
                            label = { Text("正文（运营数据 / 作者更新蓝图）") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        MetaButton(text = "发布新闻稿", onClick = vm::publishNews, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // 新闻 / 财报列表（所有读者可见）
            item {
                Spacer(Modifier.height(4.dp))
                Text("书圈公告 / 财报", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            if (state.news.isEmpty()) {
                item { Text("暂无公告。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel) }
            } else {
                items(state.news, key = { it.id }) { news -> NewsRow(news = news) }
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
private fun PanelCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SentimentChip(label: String, value: String, selected: Boolean, color: Color, onSelect: (String) -> Unit) {
    val bg = if (selected) color.copy(alpha = 0.16f) else Color.Transparent
    MetaButton(text = label, onClick = { onSelect(value) }, variant = MetaButtonVariant.Ghost)
}

@Composable
private fun NewsRow(news: NewsItemDto) {
    val sentimentColor = when (news.sentiment) {
        "bull" -> Color(0xFF2E7D32)
        "bear" -> Color(0xFFC62828)
        else -> GlassTokens.SecondaryLabel
    }
    val sentimentText = when (news.sentiment) {
        "bull" -> "利好"
        "bear" -> "利空"
        else -> "中性"
    }
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(news.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label, modifier = Modifier.weight(1f))
                Text(sentimentText, style = MaterialTheme.typography.bodySmall, color = sentimentColor, fontWeight = FontWeight.Bold)
            }
            if (news.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(news.body, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }
            Spacer(Modifier.height(4.dp))
            Text(fmtNewsTs(news.createdAt), style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
        }
    }
}

private fun fmtNewsTs(ts: Long): String {
    if (ts <= 0) return ""
    return try { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(ts)) } catch (_: Exception) { "" }
}
