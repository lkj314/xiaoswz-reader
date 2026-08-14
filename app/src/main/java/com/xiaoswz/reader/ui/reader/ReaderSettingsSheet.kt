package com.xiaoswz.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.ui.theme.ReaderThemes

/**
 * 阅读设置底部面板
 */
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // ── 主题 ──
        Text("阅读主题", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReaderThemes.forEachIndexed { index, theme ->
                val selected = settings.themeIndex == index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(theme.background)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF),
                                shape = CircleShape,
                            )
                            .clickable { onChange { it.copy(themeIndex = index) } },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 字号 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("字号", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onChange { it.copy(fontSize = it.fontSize - 1) } }) {
                Text("A-", style = MaterialTheme.typography.titleMedium)
            }
            Text("${settings.fontSize}", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onChange { it.copy(fontSize = it.fontSize + 1) } }) {
                Text("A+", style = MaterialTheme.typography.titleMedium)
            }
        }

        // ── 行距 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("行距", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Slider(
                value = settings.lineSpacing,
                onValueChange = { v -> onChange { it.copy(lineSpacing = v) } },
                valueRange = ReaderSettings.MIN_LINE_SPACING..ReaderSettings.MAX_LINE_SPACING,
                modifier = Modifier.weight(1f),
            )
            Text("%.1f".format(settings.lineSpacing), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 段距 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("段距", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("紧凑" to 0, "适中" to 1, "宽松" to 2).forEach { (label, value) ->
                    FilterChip(
                        selected = settings.paraSpacing == value,
                        onClick = { onChange { it.copy(paraSpacing = value) } },
                        label = { Text(label) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 边距 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("边距", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("窄" to 0, "标准" to 1, "宽" to 2).forEach { (label, value) ->
                    FilterChip(
                        selected = settings.marginIndex == value,
                        onClick = { onChange { it.copy(marginIndex = value) } },
                        label = { Text(label) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 翻页模式 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("翻页", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.pageMode == ReaderSettings.MODE_COVER,
                    onClick = { onChange { it.copy(pageMode = ReaderSettings.MODE_COVER) } },
                    label = { Text("覆盖") },
                )
                FilterChip(
                    selected = settings.pageMode == ReaderSettings.MODE_SCROLL,
                    onClick = { onChange { it.copy(pageMode = ReaderSettings.MODE_SCROLL) } },
                    label = { Text("滚动") },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 开关组 ──
        SettingSwitch("首行缩进", settings.indentFirstLine) { v ->
            onChange { it.copy(indentFirstLine = v) }
        }
        SettingSwitch("音量键翻页", settings.volumeKeyPaging) { v ->
            onChange { it.copy(volumeKeyPaging = v) }
        }
        SettingSwitch("屏幕常亮", settings.keepScreenOn) { v ->
            onChange { it.copy(keepScreenOn = v) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
