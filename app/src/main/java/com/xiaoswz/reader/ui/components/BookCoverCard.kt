package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.model.formatWordCount
import com.xiaoswz.reader.data.model.resolveCoverUrl

/**
 * 统一封面卡片：书城与书架共用，保证视觉一致。
 * cover(2:3) + 圆角 16 + 可选角标(badge) + 可选删除(onRemove) + 标题/作者/字数。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCoverCard(
    coverUrl: String?,
    title: String,
    author: String? = null,
    wordCount: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    /** 阅读进度百分比 0..100，>0 时在封面底部显示进度条（0.16.0） */
    progress: Int? = null,
    /** 状态角标文字（如「在读」「读完」「想读」），非空时显示在封面底部（0.16.0） */
    statusLabel: String? = null,
    /** 长按回调（书架用于弹出状态设置，0.16.0） */
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = resolveCoverUrl(coverUrl),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (badge != null) {
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { badge() }
            }
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "移出书架",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            if (statusLabel != null || (progress != null && progress > 0)) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (statusLabel != null) {
                        Box(Modifier.padding(start = 8.dp, bottom = 6.dp)) {
                            StatusPill(text = statusLabel)
                        }
                    }
                    if (progress != null && progress > 0) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val wc = formatWordCount(wordCount)
        if (!author.isNullOrBlank() || wc.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (!author.isNullOrBlank()) append(author)
                    if (wc.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(wc)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
