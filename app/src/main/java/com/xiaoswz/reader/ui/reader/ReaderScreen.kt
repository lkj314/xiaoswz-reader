package com.xiaoswz.reader.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoswz.reader.ui.theme.ReaderColors

@Composable
fun ReaderScreen(
    bookSlug: String,
    chapterId: String,
    onBack: () -> Unit,
    onChapterChange: (String) -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(chapterId) {
        viewModel.load(bookSlug, chapterId)
    }

    // 阅读时保持屏幕常亮，退出时恢复
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val backgroundColor = if (state.isDark) ReaderColors.NightBackground else ReaderColors.DayBackground
    val textColor = if (state.isDark) ReaderColors.NightText else ReaderColors.DayText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = textColor,
                )
            }
            Text(
                text = state.bookName.ifBlank { "阅读" },
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 正文区
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = state.error ?: "加载失败", color = textColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.retry(bookSlug, chapterId) }) {
                            Text("重试")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp),
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.chapterTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                        )
                        if (state.totalChapters > 0) {
                            Text(
                                text = "第 ${state.currentIndex} / ${state.totalChapters} 章",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.content,
                            fontSize = state.fontSize.sp,
                            lineHeight = (state.fontSize * 1.7).sp,
                            color = textColor,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // 底栏：翻页 + 设置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { state.prevChapterId?.let(onChapterChange) },
                enabled = state.prevChapterId != null,
            ) {
                Text("上一章", color = if (state.prevChapterId != null) textColor else textColor.copy(alpha = 0.35f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.decreaseFontSize() }) {
                    Text("A-", color = textColor)
                }
                TextButton(onClick = { viewModel.increaseFontSize() }) {
                    Text("A+", color = textColor)
                }
                TextButton(onClick = { viewModel.toggleDarkMode() }) {
                    Text(if (state.isDark) "日间" else "夜间", color = textColor)
                }
            }

            TextButton(
                onClick = { state.nextChapterId?.let(onChapterChange) },
                enabled = state.nextChapterId != null,
            ) {
                Text("下一章", color = if (state.nextChapterId != null) textColor else textColor.copy(alpha = 0.35f))
            }
        }
    }
}
