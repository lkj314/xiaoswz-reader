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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoswz.reader.data.settings.ReaderSettings
import com.xiaoswz.reader.ui.theme.ReaderBodyFont
import com.xiaoswz.reader.ui.theme.ReaderTheme
import com.xiaoswz.reader.ui.theme.ReaderThemes
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant

/**
 * 阅读设置底部面板（M2.5：分区 + 图标 + 字号实时预览样张）
 */
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onChange: ((ReaderSettings) -> ReaderSettings) -> Unit,
    // 创意工坊主题槽（3.3）：插件贡献的阅读主题，追加到内置主题之后。
    pluginThemes: List<ReaderTheme> = emptyList(),
) {
    val allThemes = ReaderThemes + pluginThemes
    val theme = allThemes.getOrElse(settings.themeIndex) { ReaderThemes[0] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // ── 阅读主题 ──
        SectionTitle("阅读主题")
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            allThemes.forEachIndexed { index, t ->
                val selected = settings.themeIndex == index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(t.background)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9CA3AF),
                                shape = CircleShape,
                            )
                            .clickable { onChange { it.copy(themeIndex = index) } },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        t.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── 实时预览样张（随字号 / 主题即时变化）──
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(theme.background)
                .padding(14.dp),
        ) {
            Text(
                text = PREVIEW_SAMPLE,
                fontFamily = ReaderBodyFont,
                fontSize = settings.fontSize.sp,
                lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                color = theme.text,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // ── 排版 ──
        SectionTitle("排版")
        Spacer(modifier = Modifier.height(8.dp))

        SettingRow(icon = Icons.Filled.FormatSize, label = "字号") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaButton(text = "A-", onClick = { onChange { it.copy(fontSize = it.fontSize - 1) } }, variant = MetaButtonVariant.Ghost)
                Text(
                    "${settings.fontSize}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                MetaButton(text = "A+", onClick = { onChange { it.copy(fontSize = it.fontSize + 1) } }, variant = MetaButtonVariant.Ghost)
            }
        }

        SettingRow(icon = Icons.Filled.VerticalAlignCenter, label = "行距") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = { v -> onChange { it.copy(lineSpacing = v) } },
                    valueRange = ReaderSettings.MIN_LINE_SPACING..ReaderSettings.MAX_LINE_SPACING,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1f".format(settings.lineSpacing),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
            }
        }

        SettingRow(icon = Icons.Filled.Subject, label = "段距") {
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

        SettingRow(icon = Icons.Filled.BorderOuter, label = "边距") {
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // ── 听书 ──
        SectionTitle("听书")
        Spacer(modifier = Modifier.height(8.dp))
        SettingRow(icon = Icons.Filled.GraphicEq, label = "语音语速") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = settings.ttsRate,
                    onValueChange = { v -> onChange { it.copy(ttsRate = v) } },
                    valueRange = ReaderSettings.MIN_TTS_RATE..ReaderSettings.MAX_TTS_RATE,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1fx".format(settings.ttsRate),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(40.dp),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // ── 护眼 ──
        SectionTitle("护眼")
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
            icon = Icons.Filled.Nightlight,
            label = "蓝光过滤",
            checked = settings.blueLightFilter,
        ) { v -> onChange { it.copy(blueLightFilter = v) } }
        SettingSwitchRow(
            icon = Icons.Filled.Timer,
            label = "休息提醒",
            checked = settings.restReminderEnabled,
        ) { v -> onChange { it.copy(restReminderEnabled = v) } }
        if (settings.restReminderEnabled) {
            SettingRow(icon = Icons.Filled.Timer, label = "间隔") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 45, 60).forEach { m ->
                        FilterChip(
                            selected = settings.restReminderMinutes == m,
                            onClick = { onChange { it.copy(restReminderMinutes = m) } },
                            label = { Text("${m}分") },
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // ── 翻页 ──
        SectionTitle("翻页")
        Spacer(modifier = Modifier.height(8.dp))
        SettingRow(icon = Icons.Filled.AutoStories, label = "翻页") {
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // ── 其他 ──
        SectionTitle("其他")
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
            icon = Icons.Filled.FormatIndentIncrease,
            label = "首行缩进",
            checked = settings.indentFirstLine,
        ) { v -> onChange { it.copy(indentFirstLine = v) } }
        SettingSwitchRow(
            icon = Icons.Filled.VolumeUp,
            label = "音量键翻页",
            checked = settings.volumeKeyPaging,
        ) { v -> onChange { it.copy(volumeKeyPaging = v) } }
        SettingSwitchRow(
            icon = Icons.Filled.Lightbulb,
            label = "屏幕常亮",
            checked = settings.keepScreenOn,
        ) { v -> onChange { it.copy(keepScreenOn = v) } }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 设置面板预览样张（固定文学示例，展现衬线/字号/主题效果） */
private const val PREVIEW_SAMPLE =
    "月光如流水一般，静静地泻在这一片叶子和花上。薄薄的青雾浮起在荷塘里。" +
    "叶子和花仿佛在牛乳中洗过一样；又像笼着轻纱的梦。虽然是满月，天上却有一层淡淡的云。"

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(64.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    SettingRow(icon = icon, label = label) {
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
