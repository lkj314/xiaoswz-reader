package com.xiaoswz.reader.ui.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// 菜单遮罩底色（所有阅读主题下都可读）
private val OverlayBg = Color(0xCC14181D)
private val OverlayText = Color.White
private val OverlayTextDim = Color.White.copy(alpha = 0.35f)

// 菜单卡片统一圆角（浮动不贴边）
private val MenuCardRadius = 16.dp

/**
 * 阅读器顶部菜单卡片：返回 + 书名 + 章节进度
 */
@Composable
fun ReaderTopBar(
    bookName: String,
    chapterProgress: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = MenuCardRadius, bottomEnd = MenuCardRadius),
        color = OverlayBg,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = OverlayText,
                )
            }
            Text(
                text = bookName.ifBlank { "阅读" },
                style = MaterialTheme.typography.titleMedium,
                color = OverlayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (chapterProgress.isNotBlank()) {
                Text(
                    text = chapterProgress,
                    style = MaterialTheme.typography.bodySmall,
                    color = OverlayText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 14.dp),
                )
            }
        }
    }
}

/**
 * 阅读器底部菜单卡片：目录 / 上一章 / 设置 / 下一章（图标按钮，禁用态半透明）
 */
@Composable
fun ReaderBottomBar(
    hasPrev: Boolean,
    hasNext: Boolean,
    onToc: () -> Unit,
    onPrev: () -> Unit,
    onSettings: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = MenuCardRadius, topEnd = MenuCardRadius),
        color = OverlayBg,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToc) {
                Icon(Icons.Filled.Menu, contentDescription = "目录", tint = OverlayText)
            }
            IconButton(onClick = onPrev, enabled = hasPrev) {
                Icon(
                    Icons.Filled.NavigateBefore,
                    contentDescription = "上一章",
                    tint = if (hasPrev) OverlayText else OverlayTextDim,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", tint = OverlayText)
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(
                    Icons.Filled.NavigateNext,
                    contentDescription = "下一章",
                    tint = if (hasNext) OverlayText else OverlayTextDim,
                )
            }
        }
    }
}
