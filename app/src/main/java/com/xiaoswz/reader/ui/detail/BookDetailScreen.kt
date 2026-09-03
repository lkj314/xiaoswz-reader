package com.xiaoswz.reader.ui.detail
import com.xiaoswz.reader.ui.components.MetaCoverImage
import com.xiaoswz.reader.ui.components.MetaAvatarImage

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.xiaoswz.reader.data.model.formatWordCount
import com.xiaoswz.reader.data.model.shrinkCover
import com.xiaoswz.reader.data.bookshelf.BookshelfRepository
import com.xiaoswz.reader.data.bookshelf.BookEntity
import com.xiaoswz.reader.data.bookshelf.BookUpdateStore
import com.xiaoswz.reader.data.backend.BackendRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.api.AdCreativeDto
import com.xiaoswz.reader.ui.components.StatusPill
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.SectionHeader
import com.xiaoswz.reader.ui.components.whaleGlassCard
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.data.booklist.BooklistRepository
import com.xiaoswz.reader.data.api.BOOK_SOURCE_MAIN
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.data.api.CharacterDto
import com.xiaoswz.reader.data.api.AuthorLogDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.xiaoswz.reader.ui.theme.MetaIcons

private fun statusText(s: String?): String = when (s) {
    "ONGOING" -> "连载中"
    "COMPLETED" -> "已完结"
    "HIATUS" -> "暂停中"
    "DROPPED" -> "已切书"
    else -> s ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    slug: String,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onBookClick: (String) -> Unit = {},
    onAccountClick: () -> Unit = {},
    onCharacterClick: (String) -> Unit = {},
    onAuthorLogClick: () -> Unit = {},
    onBookCircleClick: () -> Unit = {},
    viewModel: BookDetailViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookshelfRepo = remember { BookshelfRepository(context.applicationContext) }
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }
    val loggedIn by appSettings.isLoggedInFlow.collectAsState(initial = false)
    var collected by remember { mutableStateOf(false) }
    var hasUpdate by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    // 加入书单（0.7.4）：底部弹层选书单 / 新建书单并加入
    var showAddToBooklist by remember { mutableStateOf(false) }
    var myBooklists by remember { mutableStateOf<List<BooklistSummary>>(emptyList()) }
    var newBooklistTitle by remember { mutableStateOf("") }
    var addingToBooklist by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 作者碎碎念（0.16.5）：详情页专区数据
    var authorLogs by remember { mutableStateOf<List<AuthorLogDto>>(emptyList()) }
    var authorLogsFailed by remember { mutableStateOf(false) }

    fun doAddToBooklist(booklistId: String) {
        val d = viewModel.uiState.value.detail ?: return
        scope.launch {
            addingToBooklist = true
            BooklistRepository.addItem(
                id = booklistId,
                bookSourceId = BOOK_SOURCE_MAIN,
                bookId = slug,
                title = d.name,
                author = d.author,
                coverUrl = d.coverUrl,
                note = null,
                bookUid = null,
            ).onSuccess {
                Toast.makeText(context, "已加入书单", Toast.LENGTH_SHORT).show()
                showAddToBooklist = false
            }.onFailure { e ->
                Toast.makeText(context, e.message ?: "加入失败", Toast.LENGTH_SHORT).show()
            }
            addingToBooklist = false
        }
    }

    LaunchedEffect(showAddToBooklist) {
        if (showAddToBooklist) {
            BooklistRepository.getBooklists("mine", 1).onSuccess { resp ->
                myBooklists = resp.booklists
            }
        }
    }

    LaunchedEffect(slug) {
        viewModel.load(slug)
        collected = bookshelfRepo.isCollected(slug)
    }

    LaunchedEffect(state.detail) {
        val d = state.detail ?: return@LaunchedEffect
        val known = BookUpdateStore.getKnown(slug)
        if (known != null && (d.chapterCount ?: 0) > known) {
            BookUpdateStore.markUpdated(slug)
        }
        hasUpdate = BookUpdateStore.getHasUpdate(slug)
    }

    // 后端反馈轻量 Toast
    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 拉取作者日志（后端不可达时静默降级，专区不显示）
    LaunchedEffect(slug) {
        authorLogsFailed = false
        BackendRepository.getAuthorLogs(slug, page = 1)
            .onSuccess { resp -> authorLogs = resp.items }
            .onFailure { authorLogsFailed = true }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.detail?.name ?: "书籍详情",
                onBack = onBack,
                showLogo = false,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = MetaIcons.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.error ?: "加载失败",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    MetaButton(text = "重试", onClick = { viewModel.retry(slug) })
                }
            }

            else -> {
                val detail = state.detail ?: return@Scaffold
                val chapters = detail.chapters.orEmpty()
                val currentCount = detail.chapterCount ?: chapters.size
                val openChapter: (String?) -> Unit = { id ->
                    BookUpdateStore.markSeen(slug, currentCount)
                    hasUpdate = false
                    id?.let(onChapterClick)
                }

                // 修复（M13）：评论区之前有若干「条件 item」（角色卡 / 书圈 / 作者日志 / 简介），
                // 原先用魔法数字 `(if (introShown) 4 else 3) + chapters.size` 估算下标，
                // 条件 item 出现与否会让它偏移 1~3 位 → 「查看评论」永远滚到目录区。
                // 改为在构建列表时按实际顺序累加，得到评论头 item 的真实下标。
                // （LazyListScope 的 DSL 在组合阶段即按序执行，点击回调触发时已是最终值）
                val listState = rememberLazyListState()
                var commentHeaderIndex = 0

                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    // 书籍信息头部（玻璃卡片）
                    item {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            radius = GlassTokens.RadiusLG,
                        ) {
                            Column {
                                Row {
                                    MetaCoverImage(
                                        model = detail.coverUrl,
                                        title = detail.name.orEmpty(),
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = detail.name.orEmpty(),
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = detail.author.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            val st = statusText(detail.status)
                                            if (st.isNotBlank()) {
                                                StatusPill(text = st)
                                            }
                                            if (hasUpdate) {
                                                StatusPill(
                                                    text = "有更新",
                                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            StatusPill(
                                                text = formatWordCount(detail.wordCount),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            StatusPill(
                                                text = "${detail.chapterCount ?: chapters.size}章",
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                // 操作按钮：开始阅读整宽；加入书架 + 加入书单 等分
                                val firstChapter = chapters.firstOrNull()
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    MetaButton(
                                        text = "开始阅读",
                                        onClick = { openChapter(firstChapter?.id) },
                                        enabled = firstChapter != null,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    // 整本离线下载（0.9.5）：下载后断网可读
                                    if (state.isDownloading) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LinearProgressIndicator(
                                                progress = {
                                                    if (state.downloadTotal > 0)
                                                        state.downloadDone.toFloat() / state.downloadTotal
                                                    else 0f
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Text(
                                                "离线下载中 ${state.downloadDone}/${state.downloadTotal}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    } else {
                                        MetaButton(
                                            text = "下载整本（离线可读）",
                                            onClick = { viewModel.downloadWholeBook() },
                                            modifier = Modifier.fillMaxWidth(),
                                            variant = MetaButtonVariant.Outline,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        MetaButton(
                                            text = if (collected) "移出书架" else "加入书架",
                                            onClick = {
                                                scope.launch {
                                                    if (collected) {
                                                        bookshelfRepo.remove(slug)
                                                        collected = false
                                                    } else {
                                                        bookshelfRepo.add(
                                                            BookEntity(
                                                                slug = slug,
                                                                title = detail.name ?: "",
                                                                author = detail.author,
                                                                coverUrl = shrinkCover(detail.coverUrl),
                                                                firstChapterId = firstChapter?.id,
                                                                lastChapterId = firstChapter?.id,
                                                                lastChapterTitle = firstChapter?.name,
                                                                addedAt = System.currentTimeMillis(),
                                                                lastReadAt = System.currentTimeMillis(),
                                                            )
                                                        )
                                                        collected = true
                                                        BookUpdateStore.setKnown(slug, currentCount)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            variant = MetaButtonVariant.Outline,
                                        )
                                        MetaButton(
                                            onClick = { showAddToBooklist = true },
                                            modifier = Modifier.weight(1f),
                                            variant = MetaButtonVariant.Outline,
                                        ) {
                                            Icon(
                                                MetaIcons.PlaylistAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("加入书单", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 互动区：月票 / 评分 / 广告（P1–P3，后端不可达自动降级）──
                    commentHeaderIndex++ // 已完成：书籍信息头部
                    item {
                        InteractionCard(
                            stats = state.stats,
                            voteBalance = state.voteBalance,
                            rating = state.rating,
                            ad = state.ad,
                            commentTotal = state.commentTotal,
                            onOpenComments = {
                                scope.launch { listState.animateScrollToItem(commentHeaderIndex) }
                            },
                            onVote = { viewModel.vote() },
                            onRate = { score -> viewModel.submitRating(score) },
                            onAdClick = { ad ->
                                scope.launch { BackendRepository.reportClick(ad.id) }
                                ad.bookId?.let(onBookClick)
                            },
                        )
                    }

                    // ── 角色互动横滑卡片（0.14.0）：点击进入角色详情 ──
                    commentHeaderIndex++ // 已完成：互动卡
                    if (state.characters.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = "角色",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.characters, key = { it.id }) { ch ->
                                        CharacterChip(ch, onClick = { onCharacterClick(ch.id) })
                                    }
                                }
                            }
                        }
                    }

                    // ── 书圈（0.17.0）：以书为锚点的社区经济体入口 ──
                    commentHeaderIndex++ // 已完成：角色卡（条件 item）
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .whaleGlassCard()
                                .clickable { onBookCircleClick() },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(MetaIcons.Groups, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "书圈",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTokens.Label,
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "加入本书圈 · 竞拍圈主 · 书币投资 · 声望排行",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTokens.SecondaryLabel,
                                    )
                                }
                                Text("进入 ›", color = GlassTokens.SystemBlue, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // ── 作者碎碎念 / 作者日志（0.16.5）：作者面向读者的透明窗口 ──
                    commentHeaderIndex++ // 已完成：书圈卡
                    if (!authorLogsFailed) {
                        item {
                            AuthorLogZone(
                                logs = authorLogs,
                                onViewAll = onAuthorLogClick,
                            )
                        }
                    }

                    commentHeaderIndex++ // 已完成：作者日志（条件 item）
                    // 简介
                    if (!detail.intro.isNullOrBlank()) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "简介",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = detail.intro,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    commentHeaderIndex++ // 已完成：简介（条件 item）
                    // 目录标题
                    item {
                        HorizontalDivider()
                        Text(
                            text = "目录（共 ${chapters.size} 章）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    commentHeaderIndex++ // 已完成：目录标题
                    // 章节列表
                    items(
                        items = chapters,
                        key = { it.id ?: it.index ?: 0 },
                    ) { chapter ->
                        Text(
                            text = chapter.name.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openChapter(chapter.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    // ── 评论区（P2）──
                    // 章节列表占了 chapters.size 个 item；此处 commentHeaderIndex 即评论区头的真实下标
                    commentHeaderIndex += chapters.size
                    item {
                        HorizontalDivider()
                        SectionHeader(title = "评论（${state.commentTotal}）")
                    }
                    items(
                        items = state.comments,
                        key = { it.id },
                    ) { c ->
                        CommentRow(
                            comment = c,
                            onLike = { viewModel.likeComment(c.id) },
                            onReport = { viewModel.reportComment(c.id) },
                        )
                    }

                    // 评论输入框
                    item {
                        if (loggedIn) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("说点什么…", color = GlassTokens.SecondaryLabel) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(GlassTokens.RadiusPill),
                                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                                        focusedContainerColor = GlassTokens.GlassFillStrong,
                                        unfocusedContainerColor = GlassTokens.GlassFillStrong,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = GlassTokens.SystemBlue,
                                        focusedTextColor = GlassTokens.Label,
                                        unfocusedTextColor = GlassTokens.Label,
                                    ),
                                )
                                MetaButton(
                                    text = "发送",
                                    onClick = {
                                        viewModel.postComment(commentText)
                                        commentText = ""
                                    },
                                    enabled = commentText.isNotBlank(),
                                )
                            }
                        } else {
                            // 游客不可发评论：引导登录（评论列表仍对所有人可见）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                MetaButton(
                                    text = "登录后参与评论 ›",
                                    onClick = onAccountClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = MetaButtonVariant.Outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 加入书单底部弹层（0.7.4）
    if (showAddToBooklist) {
        ModalBottomSheet(
            onDismissRequest = { showAddToBooklist = false },
            sheetState = addSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 修复：新建书单输入框在键盘弹起时被遮挡 → 补 imePadding 把内容顶到键盘上方
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                Text(
                    text = "加入书单",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                if (myBooklists.isEmpty()) {
                    Text(
                        text = "你还没有书单，先在下方创建一个吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlassTokens.SecondaryLabel,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                    ) {
                        items(myBooklists) { bl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !addingToBooklist) { doAddToBooklist(bl.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetaCoverImage(
                                    model = bl.coverUrl,
                                    title = bl.title,
                                    modifier = Modifier
                                        .size(44.dp, 60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bl.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${bl.itemCount} 本",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlassTokens.SecondaryLabel,
                                    )
                                }
                                if (addingToBooklist) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                // 新建书单并加入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newBooklistTitle,
                        onValueChange = { newBooklistTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("新建书单名称", color = GlassTokens.SecondaryLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(GlassTokens.RadiusPill),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = GlassTokens.GlassFillStrong,
                            unfocusedContainerColor = GlassTokens.GlassFillStrong,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = GlassTokens.SystemBlue,
                            focusedTextColor = GlassTokens.Label,
                            unfocusedTextColor = GlassTokens.Label,
                        ),
                    )
                    MetaButton(
                        text = "创建并加入",
                        onClick = {
                            val title = newBooklistTitle.trim()
                            if (title.isEmpty()) return@MetaButton
                            scope.launch {
                                addingToBooklist = true
                                BooklistRepository.createBooklist(title, null, null)
                                    .onSuccess { id ->
                                        newBooklistTitle = ""
                                        doAddToBooklist(id)
                                    }
                                    .onFailure { e ->
                                        Toast.makeText(context, e.message ?: "创建失败", Toast.LENGTH_SHORT).show()
                                        addingToBooklist = false
                                    }
                            }
                        },
                        enabled = newBooklistTitle.trim().isNotEmpty() && !addingToBooklist,
                    )
                }
            }
        }
    }
}

/** 月票 / 评分 / 广告 聚合卡片 */
@Composable
private fun InteractionCard(
    stats: com.xiaoswz.reader.data.api.BookStatsResponse?,
    voteBalance: com.xiaoswz.reader.data.api.VoteBalance?,
    rating: com.xiaoswz.reader.data.api.RatingResponse?,
    ad: AdCreativeDto?,
    commentTotal: Int,
    onOpenComments: () -> Unit,
    onVote: () -> Unit,
    onRate: (Int) -> Unit,
    onAdClick: (AdCreativeDto) -> Unit,
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        radius = GlassTokens.RadiusLG,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 月票行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "本月月票 ${stats?.voteMonth ?: 0}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "累计 ${stats?.voteCount ?: 0} · 浏览 ${stats?.viewCount ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val remaining = voteBalance?.remaining ?: 0
                MetaButton(
                    text = if (remaining > 0) "投月票（剩$remaining）" else "今日已投完",
                    onClick = onVote,
                    enabled = remaining > 0,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // 评分行
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "评分 ${String.format("%.1f", rating?.avg ?: 0.0)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${rating?.count ?: 0} 人评分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val my = rating?.myScore ?: 0
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= my) MetaIcons.Star else MetaIcons.StarBorder,
                            contentDescription = null,
                            tint = if (i <= my) GlassTokens.SystemBlue else GlassTokens.SecondaryLabel,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onRate(i) },
                        )
                    }
                }
            }

            // 评论区入口：点击平滑滚动到底部评论区（避免几百章时翻到底才能评论）
            Spacer(Modifier.height(12.dp))
            MetaButton(
                text = "查看评论（${commentTotal}）›",
                onClick = onOpenComments,
                modifier = Modifier.fillMaxWidth(),
                variant = MetaButtonVariant.Outline,
            )

            // 广告栏位（P3）：详情页底部交叉推书
            if (ad != null) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassTokens.GlassFillStrong)
                        .clickable { onAdClick(ad) }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ad.title ?: "推荐阅读",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = GlassTokens.Label,
                            )
                            Text(
                                text = "广告 · 交叉推书",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTokens.SecondaryLabel,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 角色横滑卡片（0.14.0）：头像 + 名字 + 主角/配角 + 比心数 */
@Composable
private fun CharacterChip(
    ch: com.xiaoswz.reader.data.api.CharacterDto,
    onClick: () -> Unit,
) {
    LiquidGlassCard(
        modifier = Modifier
            .width(116.dp)
            .clickable { onClick() },
        radius = GlassTokens.RadiusLG,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MetaAvatarImage(
                model = ch.avatarUrl,
                name = ch.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = ch.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (ch.roleType == "main") "主角" else "配角",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.SecondaryLabel,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "❤ ${ch.heartCount}",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.SecondaryLabel,
            )
        }
    }
}

/** 单条评论 */
@Composable
private fun CommentRow(
    comment: com.xiaoswz.reader.data.api.CommentItem,
    onLike: () -> Unit,
    onReport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "读者",
            style = MaterialTheme.typography.labelMedium,
            color = GlassTokens.SecondaryLabel,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "👍 ${comment.likeCount}",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.SecondaryLabel,
                modifier = Modifier.clickable { onLike() },
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "举报",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.SecondaryLabel,
                modifier = Modifier.clickable { onReport() },
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ── 作者碎碎念专区（0.16.5）──
private val authorLogTypeLabel = mapOf(
    "musings" to "碎碎念",
    "announcement" to "公告",
    "changelog" to "章节改动",
)
private val authorLogTypeColor = mapOf(
    "musings" to Color(0xFF9B6DFF),
    "announcement" to GlassTokens.SystemBlue,
    "changelog" to Color(0xFFE0A200),
)

/** 详情页专区：列出最近 3 条作者日志；空则提示；点击「查看全部」进入时间线全屏页。 */
@Composable
private fun AuthorLogZone(
    logs: List<AuthorLogDto>,
    onViewAll: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "作者碎碎念", style = MaterialTheme.typography.titleMedium)
            if (logs.isNotEmpty()) {
                MetaButton(text = "查看全部 ›", onClick = onViewAll, variant = MetaButtonVariant.Ghost)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (logs.isEmpty()) {
            Text(
                text = "作者还没有发布碎碎念～",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTokens.SecondaryLabel,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                logs.take(3).forEach { log ->
                    AuthorLogMiniCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun AuthorLogMiniCard(log: AuthorLogDto) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth().whaleGlassCard(),
        radius = GlassTokens.RadiusLG,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    authorLogTypeLabel[log.type] ?: log.type,
                    color = authorLogTypeColor[log.type] ?: GlassTokens.SystemBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (log.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Text("置顶", color = GlassTokens.TertiaryLabel, fontSize = 11.sp)
                }
                if (!log.chapterRef.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text("📖 ${log.chapterRef}", color = GlassTokens.SecondaryLabel, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                log.title,
                color = GlassTokens.Label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                log.body,
                color = GlassTokens.SecondaryLabel,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                fmt.format(Date(log.createdAt)),
                color = GlassTokens.TertiaryLabel,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
