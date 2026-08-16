package com.xiaoswz.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.data.cache.ChapterCacheManager
import com.xiaoswz.reader.data.model.ChapterDto

/**
 * 阅读器目录抽屉内容
 */
@Composable
fun TocDrawerContent(
    bookName: String,
    toc: List<ChapterDto>,
    currentChapterId: String,
    onChapterClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentIndex = toc.indexOfFirst { it.id == currentChapterId }

    // 打开时滚动定位到当前章
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
        }
    }

    Column {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bookName.ifBlank { "目录" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "共 ${toc.size} 章",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        LazyColumn(state = listState) {
            itemsIndexed(toc, key = { _, ch -> ch.id ?: ch.index ?: 0 }) { _, chapter ->
                val isCurrent = chapter.id == currentChapterId
                // 已离线缓存且新鲜的章节：标题置灰 + 「缓存」小标，潜移默化提示（不再用顶部警示条）
                val isCached = chapter.id?.let { ChapterCacheManager.isFresh(it) } == true
                val titleColor = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCached -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface,
                        )
                        .clickable { chapter.id?.let(onChapterClick) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chapter.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCached && !isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "缓存",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
