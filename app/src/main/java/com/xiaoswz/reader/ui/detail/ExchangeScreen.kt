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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.xiaoswz.reader.data.api.ExchangeOrderDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.theme.GlassTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExchangeScreen(
    fromBookId: String,
    toBookId: String,
    onBack: () -> Unit,
) {
    val vm: ExchangeViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val nf = remember { NumberFormat.getNumberInstance(Locale.CHINA) }

    LaunchedEffect(fromBookId, toBookId) { vm.init(fromBookId, toBookId) }

    Scaffold(
        topBar = { AppTopBar(title = "书币交易所", onBack = onBack, showLogo = false) },
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
                // 交易对：可编辑书 ID + 交换
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("卖出（你的书币）", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = vm::swapPair, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SwapHoriz, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                        OutlinedTextField(
                            value = state.fromBookId,
                            onValueChange = vm::setFromBookId,
                            label = { Text("卖出书 ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("买入（目标书币）", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        OutlinedTextField(
                            value = state.toBookId,
                            onValueChange = vm::setToBookId,
                            label = { Text("买入书 ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("可用《${state.fromBookId}》 ${nf.format(state.balances[state.fromBookId] ?: 0)}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                            Text("可用《${state.toBookId}》 ${nf.format(state.balances[state.toBookId] ?: 0)}", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        }
                    }
                }
            }

            item {
                Text("价格走势（《${state.fromBookId}》锚定价）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (state.prices.isEmpty()) {
                            Text("暂无成交行情，挂单成交后生成价格点。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        } else {
                            val last = state.prices.last().price
                            val prev = if (state.prices.size > 1) state.prices[state.prices.size - 2].price else last
                            val up = last >= prev
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${String.format("%.4f", last)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (up) Color(0xFF2E7D32) else Color(0xFFC62828))
                                Spacer(Modifier.width(8.dp))
                                Text(if (up) "▲" else "▼", color = if (up) Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                            Spacer(Modifier.height(8.dp))
                            CoinPriceChart(points = state.prices)
                        }
                    }
                }
            }

            item {
                Text("挂单（限价）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
                Card(
                    modifier = Modifier.fillMaxWidth().whaleGlassCard(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("用《${state.fromBookId}》换《${state.toBookId}》。挂出即锁仓 fromAmount，成交后由对方付出 toAmount。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.fromAmount,
                            onValueChange = vm::setFromAmount,
                            label = { Text("卖出数量（${state.fromBookId}）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.toAmount,
                            onValueChange = vm::setToAmount,
                            label = { Text("期望换入（${state.toBookId}）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = vm::placeOrder, modifier = Modifier.fillMaxWidth()) { Text("挂单") }
                    }
                }
            }

            item {
                Text("订单簿（${state.orders.size} 单）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GlassTokens.Label)
            }
            if (state.orders.isEmpty()) {
                item {
                    Text("当前交易对暂无挂单。", style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
                }
            } else {
                items(state.orders, key = { it.orderId }) { order ->
                    OrderRow(order = order, isMine = order.makerId == state.myUserId, onFill = { vm.fillOrder(order.orderId) }, onCancel = { vm.cancelOrder(order.orderId) })
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
private fun OrderRow(order: ExchangeOrderDto, isMine: Boolean, onFill: () -> Unit, onCancel: () -> Unit) {
    val nf = NumberFormat.getNumberInstance(Locale.CHINA)
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
                    "1 : ${String.format("%.4f", order.rate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Text(
                    "卖 ${nf.format(order.fromAmount)} → 换 ${nf.format(order.toAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            if (isMine) {
                TextButton(onClick = onCancel) { Text("撤单", color = Color(0xFFC62828)) }
            } else {
                Button(onClick = onFill) { Text("吃单") }
            }
        }
    }
}
