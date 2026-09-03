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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.xiaoswz.reader.data.api.FundSummaryDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.NumberFormat
import java.util.Locale
import com.xiaoswz.reader.ui.theme.MetaIcons

@Composable
fun FundListScreen(
    onBack: () -> Unit,
    onFundClick: (String) -> Unit,
) {
    val vm: FundListViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    Scaffold(
        topBar = { AppTopBar(title = "理财产品市场", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(MetaIcons.Savings, null, tint = Color(0xFF2BB673), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("书圈理财产品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "各书圈董事会发行「资产包」——把多本书币按权重捆绑成一款理财产品。你用个人钱包里的书币认购份额，产品在市场中的全部收益按份额每日结算，打入你的钱包净值。资产包维持正向收益才有机会产出【书圈稳定币】（恒定兑换 10 枚 B000001），长期负收益将被退市清仓。",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
            }

            if (state.canCreate) {
                item {
                    if (state.showCreate) {
                        CreateFundCard(state, vm, nf)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().whaleGlassCard().clickable(onClick = vm::toggleCreate),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("＋ 发起一款理财产品（董事）", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.SystemBlue)
                                Icon(MetaIcons.Add, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item { Text("在售 / 我的持仓", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label) }

            if (state.funds.isEmpty()) {
                item {
                    Text(
                        "还没有任何理财产品。成为某书圈董事后，即可在此发起你的第一款资产包。",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
            } else {
                items(state.funds, key = { it.fundId }) { fund ->
                    FundCard(fund = fund, onClick = { onFundClick(fund.fundId) }, nf = nf)
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
private fun riskColor(risk: String): Color = when (risk) {
    "低" -> Color(0xFF2BB673)
    "中" -> Color(0xFFE0A200)
    else -> Color(0xFFC62828)
}

@Composable
private fun CreateFundCard(state: FundListUiState, vm: FundListViewModel, nf: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("发起理财产品", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(12.dp))

            // ① 选书圈（仅你是董事的书，杜绝 403）
            Text("① 选择你的书圈", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(8.dp))
            if (state.directorBooks.isEmpty()) {
                Text("你还没有可发起产品的书圈（需成为某书圈董事）。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.directorBooks.forEach { b ->
                        val sel = state.selectedBookId == b.bookId
                        BookChip(b.bookId, sel) { vm.selectBook(b.bookId) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ② 选模版
            Text("② 选择理财包模版", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.templates.forEach { t ->
                    TemplateCard(t, state.selectedTemplateId == t.id) {
                        if (t.unlocked) vm.selectTemplate(t.id)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ③ 命名
            OutlinedTextField(
                value = state.createName,
                onValueChange = vm::setCreateName,
                label = { Text("产品名称（如「抢救民国s1 稳健包」）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.createDesc,
                onValueChange = vm::setCreateDesc,
                label = { Text("一句话简介（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 资产构成预览
            val selTmpl = state.templates.firstOrNull { it.id == state.selectedTemplateId }
            val selTmplDef = FUND_TEMPLATES.firstOrNull { it.id == state.selectedTemplateId }
            if (state.selectedBookId.isNotEmpty() && selTmpl != null && selTmpl.unlocked && selTmplDef != null) {
                val assets = selTmplDef.buildAssets(state.selectedBookId, state.allBooks)
                Spacer(Modifier.height(12.dp))
                Text("资产构成预览", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    assets.forEach { a ->
                        AssetChip(a.assetBookId, a.weightPct)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ready = state.selectedBookId.isNotEmpty() &&
                    state.selectedTemplateId.isNotEmpty() &&
                    (state.templates.firstOrNull { it.id == state.selectedTemplateId }?.unlocked == true) &&
                    state.createName.isNotBlank()
                MetaButton(
                    text = if (state.creating) "发起中…" else "发起产品",
                    onClick = vm::createFund,
                    modifier = Modifier.weight(1f),
                    enabled = !state.creating && ready,
                )
                MetaButton(
                    text = "取消",
                    onClick = vm::toggleCreate,
                    variant = MetaButtonVariant.Ghost,
                )
            }
        }
    }
}

@Composable
private fun BookChip(bookId: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GlassTokens.SystemBlue.copy(alpha = 0.18f) else GlassTokens.SystemBlue.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            "《$bookId》",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) GlassTokens.SystemBlue else GlassTokens.SecondaryLabel,
        )
    }
}

@Composable
private fun AssetChip(bookId: String, weight: Double) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF9B6DFF).copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text("$bookId ${weight.toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9B6DFF), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TemplateCard(t: FundTemplateUi, selected: Boolean, onClick: () -> Unit) {
    val enabled = t.unlocked
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .then(if (selected) Modifier.background(GlassTokens.SystemBlue.copy(alpha = 0.10f)) else Modifier)
                .padding(if (selected) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor(t.risk).copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("${t.risk}风险", style = MaterialTheme.typography.bodySmall, color = riskColor(t.risk), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(t.tagline, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                if (!enabled) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MetaIcons.Lock, null, tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("未解锁 · ${t.unlockHint}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    }
                }
            }
            if (selected) {
                Icon(MetaIcons.Star, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FundCard(fund: FundSummaryDto, onClick: () -> Unit, nf: NumberFormat) {
    val up = fund.lastDayReturn >= 0
    val cumUp = fund.cumulativeReturnPct >= 0
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fund.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "《${fund.bookId}》· NAV ${String.format("%.3f", fund.navPerShare)} · ${if (fund.status == "active") "运行中" else "已退市"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
                Icon(
                    if (up) MetaIcons.TrendingUp else MetaIcons.TrendingDown,
                    null,
                    tint = if (up) Color(0xFF2BB673) else Color(0xFFC62828),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FundStat("当日", (if (up) "+" else "") + "%.1f%%".format(fund.lastDayReturn), if (up) Color(0xFF2BB673) else Color(0xFFC62828))
                FundStat("累计", (if (cumUp) "+" else "") + "%.1f%%".format(fund.cumulativeReturnPct), if (cumUp) Color(0xFF2BB673) else Color(0xFFC62828))
                FundStat("稳定币", fund.stableCount.toString(), Color(0xFFE0A200))
                FundStat("总份额", nf.format(fund.totalShares.toLong()), GlassTokens.Label)
            }
            if (fund.myShares > 0) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GlassTokens.SystemBlue.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "我的：${nf.format(fund.myShares.toLong())} 份 · 市值 ${nf.format(fund.myValue.toLong())} · ${(if (fund.myYieldPct >= 0) "+" else "")}${"%.1f%%".format(fund.myYieldPct)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTokens.SystemBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FundStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
    }
}
