package com.xiaoswz.reader.ui.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.BadgeDto
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens

@Composable
fun ReadingStatsScreen(
    onBack: () -> Unit,
    onAccountClick: () -> Unit,
    viewModel: ReadingStatsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { AppTopBar(title = "阅读成就", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        if (state.isLoading && state.stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassTokens.SystemBlue)
            }
            return@Scaffold
        }
        val stats = state.stats
        if (stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "请登录后查看阅读成就", color = GlassTokens.SecondaryLabel)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(GlassTokens.GradientButton)
                            .clickable { onAccountClick() }.padding(horizontal = 24.dp, vertical = 10.dp),
                    ) { Text("去登录", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // 等级主卡
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(GlassTokens.RadiusXL))
                        .background(GlassTokens.GradientButton)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lv.${stats.level}", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("阅读等级", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("${stats.streakDays}", "连续打卡(天)", Modifier.weight(1f))
                    MetricCard("${stats.days}", "累计打卡(天)", Modifier.weight(1f))
                    MetricCard("${stats.totalMin / 60}h", "累计阅读", Modifier.weight(1f))
                }
            }
            item {
                Text("徽章", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
            }
            if (stats.badges.isEmpty()) {
                item { Text("暂无徽章，继续阅读解锁吧", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel) }
            } else {
                items(stats.badges, key = { it.key }) { badge ->
                    BadgeRow(badge = badge)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(GlassTokens.RadiusLG)).background(GlassTokens.GlassFillStrong).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
    }
}

@Composable
private fun BadgeRow(badge: BadgeDto) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(if (badge.unlocked) GlassTokens.Gold else GlassTokens.GroupedBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = if (badge.unlocked) Color.White else GlassTokens.TertiaryLabel,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(badge.name, style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, fontWeight = FontWeight.Medium)
            badge.desc?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel)
            }
        }
        Text(if (badge.unlocked) "已解锁" else "未解锁", style = MaterialTheme.typography.labelSmall, color = if (badge.unlocked) GlassTokens.Mint else GlassTokens.TertiaryLabel)
    }
}
