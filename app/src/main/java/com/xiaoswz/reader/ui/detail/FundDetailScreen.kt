package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.xiaoswz.reader.data.api.FundAssetDto
import com.xiaoswz.reader.data.api.FundYieldPointDto
import com.xiaoswz.reader.data.api.PricePointDto
import com.xiaoswz.reader.data.api.StableCoinDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.xiaoswz.reader.ui.theme.MetaIcons

@Composable
fun FundDetailScreen(
    fundId: String,
    onBack: () -> Unit,
) {
    val vm: FundDetailViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    LaunchedEffect(fundId) { vm.init(fundId) }

    Scaffold(
        topBar = { AppTopBar(title = state.fund?.name ?: "理财产品详情", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        val fund = state.fund
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (fund == null) {
                item {
                    Text(
                        if (state.error != null) state.error!! else "正在加载产品详情…",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.error != null) Color(0xFFC62828) else GlassTokens.SecondaryLabel,
                    )
                }
            } else {
                // 头部：NAV + 收益概览
                item { FundHeaderCard(fund = fund) }

                // 我的持仓
                if (fund.myShares > 0) {
                    item { MyHoldingCard(fund = fund) }
                }

                // 净值走势
                item { NavHistoryCard(history = fund.history) }

                // 资产包构成
                item { AssetBasketCard(assets = fund.assets) }

                // 已产出的稳定币
                item { StableListCard(stablecoins = fund.stablecoins, nf = nf) }

                // 认购 / 赎回
                item { SubscribeCard(state = state, vm = vm) }

                // 董事操作
                if (state.isDirector) {
                    item { DirectorPanelCard(state = state, vm = vm) }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(MetaIcons.Lock, null, tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("董事专属操作", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "每日「抓取稳定币」与「稳定币调拨」仅向本书圈董事开放。提升投资份额获得董事席位即可解锁。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            }
                        }
                    }
                }

                // 全局稳定币状态
                item { StableReserveCard(reserve = fund.stableReserve) }
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
private fun FundHeaderCard(fund: com.xiaoswz.reader.data.api.FundDetailResponse) {
    val up = fund.lastDayReturn >= 0
    val cumUp = fund.cumulativeReturnPct >= 0
    val warn = fund.consecutiveNegDays >= 7
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fund.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                    Spacer(Modifier.height(2.dp))
                    Text("《${fund.bookId}》· ${if (fund.status == "active") "运行中" else "已退市"}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
                Icon(
                    if (up) MetaIcons.TrendingUp else MetaIcons.TrendingDown,
                    null,
                    tint = if (up) Color(0xFF2BB673) else Color(0xFFC62828),
                    modifier = Modifier.size(28.dp),
                )
            }
            if (!fund.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(fund.description, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }
            Spacer(Modifier.height(16.dp))
            Text("每份净值（NAV）", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            Text(String.format("%.3f", fund.navPerShare), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FundStat("当日收益", (if (up) "+" else "") + "%.2f%%".format(fund.lastDayReturn), if (up) Color(0xFF2BB673) else Color(0xFFC62828))
                FundStat("累计收益", (if (cumUp) "+" else "") + "%.1f%%".format(fund.cumulativeReturnPct), if (cumUp) Color(0xFF2BB673) else Color(0xFFC62828))
                FundStat("总份额", fmt(fund.totalShares), GlassTokens.Label)
                FundStat("连负天数", fund.consecutiveNegDays.toString(), if (warn) Color(0xFFC62828) else GlassTokens.SecondaryLabel)
            }
            if (warn) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFC62828).copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("⚠ 连续负收益 ${fund.consecutiveNegDays} 天（退市阈值 14 天 / 累计 −30%），请关注清仓风险。", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MyHoldingCard(fund: com.xiaoswz.reader.data.api.FundDetailResponse) {
    val up = fund.myYieldPct >= 0
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("我的持仓", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FundStat("持有份额", fmt(fund.myShares), GlassTokens.Label)
                FundStat("当前市值", fmt(fund.myValue), Color(0xFFE0A200))
                FundStat("投入成本", fmt(fund.myCostBasis), GlassTokens.SecondaryLabel)
                FundStat("收益率", (if (up) "+" else "") + "%.1f%%".format(fund.myYieldPct), if (up) Color(0xFF2BB673) else Color(0xFFC62828))
            }
        }
    }
}

@Composable
private fun NavHistoryCard(history: List<FundYieldPointDto>) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("净值走势（NAV / 份）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(10.dp))
            val points = history.map { PricePointDto(price = it.navPerShare.toFloat()) }
            CoinPriceChart(points = points, modifier = Modifier.fillMaxWidth(), lineColor = GlassTokens.SystemBlue)
            if (points.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("暂无净值历史，待每日结算后生成。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }
        }
    }
}

@Composable
private fun AssetBasketCard(assets: List<FundAssetDto>) {
    val palette = listOf(Color(0xFF4C8DFF), Color(0xFF2BB673), Color(0xFFE0A200), Color(0xFF9B6DFF), Color(0xFFFF8A65))
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("资产包构成", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(10.dp))
            if (assets.isEmpty()) {
                Text("暂无资产配置。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            } else {
                // 权重堆叠条
                val totalWeight = assets.sumOf { it.weightPct }.toFloat().coerceAtLeast(0.0001f)
                Row(
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                ) {
                    assets.forEachIndexed { i, a ->
                        Box(
                            modifier = Modifier
                                .weight((a.weightPct.toFloat() / totalWeight).coerceAtLeast(0.0001f))
                                .fillMaxSize()
                                .background(palette[i % palette.size]),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                assets.forEachIndexed { i, a ->
                    val color = palette[i % palette.size]
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (a.assetType == "stablecoin") "稳定币" else "书币《${a.assetBookId}》",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTokens.Label,
                            )
                        }
                        Text(
                            "权重 ${"%.0f".format(a.weightPct)}% · 市值 ${fmt(a.value)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTokens.SecondaryLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StableListCard(stablecoins: List<StableCoinDto>, nf: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MetaIcons.Star, null, tint = Color(0xFFE0A200), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("已产出的稳定币（${stablecoins.size}）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            Spacer(Modifier.height(10.dp))
            if (stablecoins.isEmpty()) {
                Text("本产品暂未产出稳定币。维持正向收益、每日由董事抓取，即有概率挖出。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            } else {
                stablecoins.forEach { sc ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("稳定币 #${sc.serial}", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
                        Text("${fmt(10.0)} 枚 B000001 · ${fmtTs(sc.producedAt)}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscribeCard(state: FundDetailUiState, vm: FundDetailViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("认购 / 赎回", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            Spacer(Modifier.height(4.dp))
            Text("用你钱包里的某书币按当前 NAV 认购份额；赎回按比例回收书币（稳定币作为产品底层不退还）。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.subscribeBookId,
                onValueChange = vm::setSubscribeBookId,
                label = { Text("支付书 ID（如 B000001）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.subscribeAmount,
                onValueChange = vm::setSubscribeAmount,
                label = { Text("支付数量") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MetaButton(text = if (state.busy) "处理中…" else "认购", onClick = vm::subscribe, modifier = Modifier.fillMaxWidth(), enabled = !state.busy)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.redeemShares,
                    onValueChange = vm::setRedeemShares,
                    label = { Text("赎回份额") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                MetaButton(text = "赎回", onClick = vm::redeem, enabled = !state.busy)
            }
        }
    }
}

@Composable
private fun DirectorPanelCard(state: FundDetailUiState, vm: FundDetailViewModel) {
    val prob = state.fund?.stableReserve?.currentProbability ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MetaIcons.Lock, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("董事操作台", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            Spacer(Modifier.height(10.dp))
            Text("① 抓取稳定币（每产品每日一次）：当前全池产出概率 ${"%.3f".format(prob * 100)}%。需本产品当日收益为正，且未触及 1 万枚硬顶。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            Spacer(Modifier.height(8.dp))
            MetaButton(text = if (state.busy) "处理中…" else "抓取稳定币", onClick = vm::grab, modifier = Modifier.fillMaxWidth(), enabled = !state.busy)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("② 稳定币调拨：把本产品挖到的稳定币转入其它产品，组建「稳定币 + 书币」超级资产包。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            if (state.showTransfer) {
                OutlinedTextField(
                    value = state.transferSerial,
                    onValueChange = vm::setTransferSerial,
                    label = { Text("稳定币序号 #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.transferToFund,
                    onValueChange = vm::setTransferToFund,
                    label = { Text("目标产品 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaButton(text = "确认转移", onClick = vm::transfer, modifier = Modifier.weight(1f), enabled = !state.busy)
                    MetaButton(text = "收起", onClick = vm::toggleTransfer, modifier = Modifier.weight(1f), variant = MetaButtonVariant.Ghost)
                }
            } else {
                MetaButton(text = "展开调拨表单", onClick = vm::toggleTransfer, variant = MetaButtonVariant.Ghost)
            }
        }
    }
}

@Composable
private fun StableReserveCard(reserve: com.xiaoswz.reader.data.api.StableReserveDto?) {
    Card(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MetaIcons.Savings, null, tint = Color(0xFFE0A200), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("书圈稳定币全局状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            Spacer(Modifier.height(10.dp))
            if (reserve == null) {
                Text("稳定币储备系统初始化中。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            } else {
                val frac = if (reserve.hardCap > 0) (reserve.issuedCount.toFloat() / reserve.hardCap).coerceIn(0f, 1f) else 0f
                Text("已产出 ${fmt(reserve.issuedCount.toDouble())} / 硬顶 ${fmt(reserve.hardCap.toDouble())}（恒定兑换 10 枚 B000001 / 枚）", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFFE0A200), trackColor = GlassTokens.SecondaryLabel.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FundStat("锁定 B000001", fmt(reserve.lockedB000001.toDouble()), GlassTokens.SystemBlue)
                    FundStat("当前概率", "%.3f%%".format(reserve.currentProbability * 100), Color(0xFF9B6DFF))
                    FundStat("单枚锚定", fmt(reserve.backing.toDouble()), Color(0xFF2BB673))
                }
                Spacer(Modifier.height(8.dp))
                Text("概率衰减：前 100 枚 100%；100 枚后骤降至 1%；1000 枚后约 0.1%，越接近 1 万越低（公式 max(0.0005, 0.001·(1−n/10000)²)）。稳定币由真实 B000001 1:10 背书，总量守恒，绝不再增发。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
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

private fun fmtTs(ts: Long): String {
    if (ts <= 0) return ""
    return try { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts)) } catch (_: Exception) { "" }
}
