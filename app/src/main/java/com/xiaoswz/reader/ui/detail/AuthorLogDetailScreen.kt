package com.xiaoswz.reader.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.data.api.AuthorLogDto
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val logTypeLabel = mapOf(
    "musings" to "碎碎念",
    "announcement" to "公告",
    "changelog" to "章节改动",
)
private val logTypeColor = mapOf(
    "musings" to Color(0xFF9B6DFF),
    "announcement" to GlassTokens.SystemBlue,
    "changelog" to Color(0xFFE0A200),
)
private val logFilters = listOf(
    null to "全部",
    "musings" to "碎碎念",
    "announcement" to "公告",
    "changelog" to "章节改动",
)

/**
 * 作者日志全屏时间线（0.16.5）：读者从详情页「作者碎碎念」专区点进来，
 * 看某本书作者的全部碎碎念 / 公告 / 章节改动。后端不可达时显示降级提示。
 */
@Composable
fun AuthorLogDetailScreen(
    bookId: String,
    bookTitle: String?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<AuthorLogDto>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun load(reset: Boolean) {
        if (reset) { page = 1; logs = emptyList(); total = 0; failed = false }
        scope.launch {
            isLoading = true
            BackendRepository.getAuthorLogs(bookId, selectedType, page)
                .onSuccess { resp ->
                    logs = if (reset) resp.items else logs + resp.items
                    total = resp.total
                    page += 1
                }
                .onFailure { failed = true }
            isLoading = false
        }
    }

    LaunchedEffect(selectedType) { load(true) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "作者碎碎念",
                onBack = onBack,
                showLogo = false,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // 类型筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                logFilters.forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }

            bookTitle?.let {
                Text(
                    it,
                    color = GlassTokens.SecondaryLabel,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            if (failed) {
                Text(
                    "日志加载失败，请稍后重试。",
                    color = GlassTokens.SecondaryLabel,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else if (logs.isEmpty() && !isLoading) {
                Text(
                    "作者还没有发布碎碎念～",
                    color = GlassTokens.SecondaryLabel,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(logs, key = { it.id }) { log ->
                        AuthorLogTimelineCard(log = log)
                    }
                    if (logs.size < total && !isLoading) {
                        item {
                            MetaButton(
                                text = "加载更多 ›",
                                onClick = { load(false) },
                                modifier = Modifier.fillMaxWidth(),
                                variant = MetaButtonVariant.Ghost,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorLogTimelineCard(log: AuthorLogDto) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        radius = GlassTokens.RadiusLG,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    logTypeLabel[log.type] ?: log.type,
                    color = logTypeColor[log.type] ?: GlassTokens.SystemBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (log.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Text("置顶", color = GlassTokens.TertiaryLabel, fontSize = 11.sp)
                }
                if (!log.chapterRef.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "📖 ${log.chapterRef}",
                        color = GlassTokens.SecondaryLabel,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    fmt.format(Date(log.createdAt)),
                    color = GlassTokens.TertiaryLabel,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                log.title,
                color = GlassTokens.Label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                log.body,
                color = GlassTokens.SecondaryLabel,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
