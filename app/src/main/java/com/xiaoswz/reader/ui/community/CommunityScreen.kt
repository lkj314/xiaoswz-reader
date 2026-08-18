package com.xiaoswz.reader.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.api.PostCommentItem
import com.xiaoswz.reader.data.api.PostDetail
import com.xiaoswz.reader.data.api.PostItem
import com.xiaoswz.reader.data.community.CommunityRepository
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.data.social.SocialRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.ReportSheet
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.WhaleColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CommunityScreen(
    onAccountClick: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onBooklistClick: (String) -> Unit = {},
    onReadingStats: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { AppSettingsRepository(context.applicationContext) }
    val accountRole by settingsRepo.accountRoleFlow.collectAsState(initial = "guest")
    val accountId by settingsRepo.accountIdFlow.collectAsState(initial = null)
    val isLoggedIn = accountRole != "guest"
    val isAdmin = accountRole == "admin"
    LaunchedEffect(accountId) { viewModel.setAccountId(accountId) }

    val listState = rememberLazyListState()
    var showPublish by remember { mutableStateOf(false) }
    var detailPostId by remember { mutableStateOf<String?>(null) }
    var editPostId by remember { mutableStateOf<String?>(null) }

    // 滚动接近底部自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "书友圈",
                showLogo = true,
                actions = {
                    IconButton(onClick = onReadingStats) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "阅读成就",
                            tint = GlassTokens.Label,
                        )
                    }
                    IconButton(onClick = { showPublish = true }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "发帖",
                            tint = GlassTokens.Label,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        val refreshing = state.isLoading && state.posts.isEmpty()
        val pullState = rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = { viewModel.refresh() },
        )
        Column(modifier = Modifier.padding(padding)) {
            // 流切换：广场 / 关注
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.feed == "square",
                    onClick = { viewModel.switchFeed("square") },
                    label = { Text("广场") },
                )
                FilterChip(
                    selected = state.feed == "following",
                    onClick = { viewModel.switchFeed("following") },
                    label = { Text("关注") },
                )
                FilterChip(
                    selected = state.feed == "hot",
                    onClick = { viewModel.switchFeed("hot") },
                    label = { Text("热门") },
                )
            }

            // 话题筛选（0.16.0）：横向滚动 chips
            if (state.topics.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedTopicId == null,
                        onClick = { viewModel.selectTopic(null) },
                        label = { Text("全部话题") },
                    )
                    state.topics.forEach { t ->
                        FilterChip(
                            selected = state.selectedTopicId == t.id,
                            onClick = { viewModel.selectTopic(t.id) },
                            label = { Text("#${t.name}") },
                        )
                    }
                }
            }

            // 关键词搜索（0.16.0）
            var keyword by remember { mutableStateOf(state.keyword ?: "") }
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("搜索动态正文…", color = GlassTokens.SecondaryLabel) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.setKeyword(keyword) }) {
                        Icon(Icons.Default.Send, contentDescription = "搜索", tint = GlassTokens.Label)
                    }
                },
                shape = RoundedCornerShape(GlassTokens.RadiusPill),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GlassTokens.GlassFill,
                    unfocusedContainerColor = GlassTokens.GlassFill,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = GlassTokens.SystemBlue,
                    focusedTextColor = GlassTokens.Label,
                    unfocusedTextColor = GlassTokens.Label,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullState),
            ) {
                when {
                    // 关注流且未登录：引导登录
                    state.feed == "following" && !isLoggedIn -> {
                        CommunityLoginHint(onAccountClick = onAccountClick)
                    }
                    state.posts.isEmpty() && state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WhaleColors.WhaleBlue)
                        }
                    }
                    state.posts.isEmpty() && state.error != null -> {
                        CommunityErrorState(error = state.error ?: "加载失败") {
                            viewModel.refresh()
                        }
                    }
                    state.posts.isEmpty() -> {
                        CommunityEmptyState()
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                if (state.feed == "square") {
                                    HomeSection(onBooklistClick = onBooklistClick)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            items(state.posts, key = { it.id }) { post ->
                                val isOwner = post.author.id == accountId
                                PostCard(
                                    post = post,
                                    isAdmin = isAdmin,
                                    isOwner = isOwner,
                                    onClick = { detailPostId = post.id },
                                    onUserClick = onUserClick,
                                    onBooklistClick = onBooklistClick,
                                    onLike = {
                                        if (!isLoggedIn) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "请先登录后再点赞",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                            onAccountClick()
                                        } else {
                                            viewModel.toggleLike(post.id)
                                        }
                                    },
                                    onEditPost = { editPostId = post.id },
                                    onDeletePost = { pid ->
                                        scope.launch {
                                            val result = if (isOwner) {
                                                CommunityRepository.deleteOwnPost(pid)
                                            } else {
                                                CommunityRepository.deletePost(pid)
                                            }
                                            result.onSuccess {
                                                viewModel.removePost(pid)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "已删除动态",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                                .onFailure {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        it.message ?: "删除失败",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                        }
                                    },
                                )
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(26.dp),
                                            color = WhaleColors.WhaleBlue,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = WhaleColors.WhaleBlue,
                )
            }
        }
    }

    // 发帖底部弹层
    if (showPublish) {
        PublishSheet(
            isLoggedIn = isLoggedIn,
            onAccountClick = onAccountClick,
            onDismiss = { showPublish = false },
            onPublished = {
                showPublish = false
                viewModel.onPostPublished()
            },
        )
    }

    // 详情 / 评论底部弹层
    detailPostId?.let { id ->
        PostDetailSheet(
            postId = id,
            isAdmin = isAdmin,
            accountId = accountId,
            viewModel = viewModel,
            onDismiss = { detailPostId = null },
            onEditPost = { editPostId = id },
            onDeleted = {
                viewModel.removePost(id)
                detailPostId = null
                android.widget.Toast.makeText(context, "已删除动态", android.widget.Toast.LENGTH_SHORT).show()
            },
            onUserClick = onUserClick,
            onBooklistClick = onBooklistClick,
        )
    }

    // 编辑动态底部弹层
    editPostId?.let { id ->
        EditPostSheet(
            postId = id,
            onDismiss = { editPostId = null },
            onSaved = {
                editPostId = null
                viewModel.refresh()
                android.widget.Toast.makeText(context, "已保存", android.widget.Toast.LENGTH_SHORT).show()
            },
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  帖子卡片
// ════════════════════════════════════════════════════════════════
@Composable
private fun PostCard(
    post: PostItem,
    isAdmin: Boolean,
    isOwner: Boolean = false,
    onClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onBooklistClick: (String) -> Unit,
    onLike: () -> Unit,
    onEditPost: (String) -> Unit = {},
    onDeletePost: (String) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        // 作者行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = post.author.avatarUrl, size = 36, modifier = Modifier.clickable { onUserClick(post.author.id) })
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = post.author.displayName ?: "读者",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GlassTokens.Label,
                )
                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTokens.SecondaryLabel,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // 正文
        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyLarge,
            color = GlassTokens.Label,
            lineHeight = 22.sp,
        )
        // 图片网格
        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            PostImageGrid(urls = post.imageUrls)
        }
        // 关联书籍 / 书单 / 话题（后端字段已支持，MVP 仅展示 chips）
        if (post.bookId != null || post.topic != null || post.booklist != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                post.topic?.let {
                    Text(
                        "#${it.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTokens.SystemBlue,
                    )
                }
                if (post.bookId != null) {
                    Text(
                        "关联书籍",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTokens.SecondaryLabel,
                    )
                }
                post.booklist?.let { ref ->
                    Text(
                        "📚 ${ref.title ?: "书单"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTokens.SystemBlue,
                        modifier = Modifier.clickable { onBooklistClick(ref.id) },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // 操作行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onLike)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (post.liked) GlassTokens.Rose else GlassTokens.SecondaryLabel,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = post.likeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "评论",
                    tint = GlassTokens.SecondaryLabel,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = post.commentCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            if (isOwner) {
                Spacer(Modifier.width(16.dp))
                Row(
                    modifier = Modifier
                        .clickable { onEditPost(post.id) }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑动态",
                        tint = GlassTokens.Label,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "编辑",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTokens.Label,
                    )
                }
            }
            if (isAdmin || isOwner) {
                Spacer(Modifier.width(16.dp))
                Row(
                    modifier = Modifier
                        .clickable { showDeleteConfirm = true }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除动态",
                        tint = GlassTokens.Rose,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTokens.Rose,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePost(post.id)
                    },
                ) { Text("删除", color = GlassTokens.Rose) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
            title = { Text("删除动态", color = GlassTokens.Label) },
            text = { Text("确定删除这条动态吗？此操作不可恢复（含其全部评论）。", color = GlassTokens.SecondaryLabel) },
        )
    }
}

@Composable
private fun Avatar(url: String?, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GlassTokens.GroupedBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = GlassTokens.TertiaryLabel,
                modifier = Modifier.size((size * 0.8f).dp),
            )
        }
    }
}

@Composable
private fun PostImageGrid(urls: List<String>) {
    val cols = if (urls.size == 1) 1 else if (urls.size <= 4) 2 else 3
    val chunks = urls.chunked(cols)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        chunks.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                // 补齐该行剩余占位，保持方格对齐
                repeat(cols - row.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  发帖底部弹层
// ════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishSheet(
    isLoggedIn: Boolean,
    onAccountClick: () -> Unit,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var imageText by remember { mutableStateOf("") }
    var publishing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "发布动态",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = GlassTokens.Label)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!isLoggedIn) {
                // 未登录：提示去登录（发帖需登录）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                        .background(GlassTokens.GroupedBackground)
                        .padding(16.dp),
                ) {
                    Text(
                        "发帖需要先登录账号",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlassTokens.Label,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                            .background(GlassTokens.GradientButton)
                            .clickable { onAccountClick() }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) {
                        Text("去登录", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 2000) content = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    placeholder = { Text("分享你的读书心得…", color = GlassTokens.SecondaryLabel) },
                    label = { Text("正文") },
                    singleLine = false,
                    shape = RoundedCornerShape(GlassTokens.RadiusMD),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassTokens.GlassFill,
                        unfocusedContainerColor = GlassTokens.GlassFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GlassTokens.SystemBlue,
                        focusedTextColor = GlassTokens.Label,
                        unfocusedTextColor = GlassTokens.Label,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = imageText,
                    onValueChange = { imageText = it },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    placeholder = {
                        Text("可选，每行一个图片链接(http/https)", color = GlassTokens.SecondaryLabel)
                    },
                    label = { Text("配图链接") },
                    singleLine = false,
                    shape = RoundedCornerShape(GlassTokens.RadiusMD),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassTokens.GlassFill,
                        unfocusedContainerColor = GlassTokens.GlassFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GlassTokens.SystemBlue,
                        focusedTextColor = GlassTokens.Label,
                        unfocusedTextColor = GlassTokens.Label,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                val publishEnabled = content.isBlank().not() && !publishing
                val publishBrush: Brush = if (publishEnabled) {
                    GlassTokens.GradientButton
                } else {
                    SolidColor(GlassTokens.TertiaryLabel)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                        .background(publishBrush)
                        .clickable(enabled = publishEnabled) {
                            val urls = imageText
                                .split("\n")
                                .map { it.trim() }
                                .filter { it.startsWith("http", ignoreCase = true) }
                                .take(9)
                            publishing = true
                            scope.launch {
                                CommunityRepository.createPost(content.trim(), urls)
                                    .onSuccess { onPublished() }
                                    .onFailure { e ->
                                        publishing = false
                                        android.widget.Toast.makeText(
                                            context,
                                            e.message ?: "发布失败",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                            }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (publishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("发布", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  编辑动态底部弹层（0.16.0 作者自改闭环）
// ════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPostSheet(
    postId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var imageText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        CommunityRepository.getPostDetail(postId)
            .onSuccess { d ->
                content = d.content
                imageText = d.imageUrls.joinToString("\n")
                loaded = true
            }
            .onFailure {
                android.widget.Toast.makeText(context, "加载失败", android.widget.Toast.LENGTH_SHORT).show()
                onDismiss()
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "编辑动态",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = GlassTokens.Label)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!loaded) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WhaleColors.WhaleBlue)
                }
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 2000) content = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    placeholder = { Text("分享你的读书心得…", color = GlassTokens.SecondaryLabel) },
                    label = { Text("正文") },
                    singleLine = false,
                    shape = RoundedCornerShape(GlassTokens.RadiusMD),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassTokens.GlassFill,
                        unfocusedContainerColor = GlassTokens.GlassFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GlassTokens.SystemBlue,
                        focusedTextColor = GlassTokens.Label,
                        unfocusedTextColor = GlassTokens.Label,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = imageText,
                    onValueChange = { imageText = it },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    placeholder = { Text("可选，每行一个图片链接(http/https)", color = GlassTokens.SecondaryLabel) },
                    label = { Text("配图链接") },
                    singleLine = false,
                    shape = RoundedCornerShape(GlassTokens.RadiusMD),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = GlassTokens.GlassFill,
                        unfocusedContainerColor = GlassTokens.GlassFill,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GlassTokens.SystemBlue,
                        focusedTextColor = GlassTokens.Label,
                        unfocusedTextColor = GlassTokens.Label,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                val saveEnabled = content.isBlank().not() && !saving
                val saveBrush: Brush = if (saveEnabled) GlassTokens.GradientButton else SolidColor(GlassTokens.TertiaryLabel)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                        .background(saveBrush)
                        .clickable(enabled = saveEnabled) {
                            val urls = imageText.split("\n").map { it.trim() }
                                .filter { it.startsWith("http", ignoreCase = true) }.take(9)
                            saving = true
                            scope.launch {
                                CommunityRepository.editPost(postId, content.trim(), urls)
                                    .onSuccess { onSaved() }
                                    .onFailure {
                                        saving = false
                                        android.widget.Toast.makeText(
                                            context,
                                            it.message ?: "保存失败",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                            }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("保存", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  详情 / 评论底部弹层
// ════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailSheet(
    postId: String,
    isAdmin: Boolean = false,
    accountId: String? = null,
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit,
    onEditPost: () -> Unit = {},
    onDeleted: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onBooklistClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<PostDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    val refreshDetail: () -> Unit = {
        loading = true
        scope.launch {
            CommunityRepository.getPostDetail(postId)
                .onSuccess { detail = it; loading = false }
                .onFailure { loading = false }
        }
    }
    LaunchedEffect(postId) { refreshDetail() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                // 0.15.4 修：当帖子带大图时，键盘弹起后底部评论输入框会被键盘盖住。
                // 加 imePadding() 让 sheet 内容让出 IME 高度，输入框始终浮在键盘上方。
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "动态详情",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Spacer(Modifier.weight(1f))
                detail?.let { d ->
                    val isOwner = d.author.id == accountId
                    if (isOwner) {
                        IconButton(onClick = onEditPost) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑动态", tint = GlassTokens.Label)
                        }
                    }
                    if (isOwner || isAdmin) {
                        IconButton(onClick = {
                            scope.launch {
                                val result = if (isOwner) {
                                    CommunityRepository.deleteOwnPost(d.id)
                                } else {
                                    CommunityRepository.deletePost(d.id)
                                }
                                result.onSuccess { onDeleted() }
                                    .onFailure {
                                        android.widget.Toast.makeText(
                                            context,
                                            it.message ?: "删除失败",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除动态", tint = GlassTokens.Rose)
                        }
                    }
                }
                IconButton(onClick = { showReport = true }) {
                    Icon(Icons.Default.Warning, contentDescription = "举报", tint = GlassTokens.Label)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = GlassTokens.Label)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (loading) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WhaleColors.WhaleBlue)
                }
                return@ModalBottomSheet
            }

            detail?.let { d ->
                // 列表用 weight 吸收键盘弹起后的高度收缩（与 BookDetailScreen 一致）；
                // fill=false + heightIn(max) 让短内容保持紧凑、长内容（带图帖子）在键盘弹出时收缩，
                // 底部评论输入框作为固定页脚由 imePadding() 顶起，始终浮在键盘上方。
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(url = d.author.avatarUrl, size = 36, modifier = Modifier.clickable { onUserClick(d.author.id) })
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    d.author.displayName ?: "读者",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = GlassTokens.Label,
                                )
                                Text(
                                    formatRelativeTime(d.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            }
                        }
                    }
                    // 关联书单（0.7.4 分享书单的动态）
                    if (d.booklist != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                                    .background(GlassTokens.GlassFill)
                                    .clickable { onBooklistClick(d.booklist!!.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.MenuBook, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("📚 ${d.booklist!!.title ?: "书单"}", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SystemBlue)
                            }
                        }
                    }
                    item {
                        Text(
                            d.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = GlassTokens.Label,
                            lineHeight = 22.sp,
                        )
                    }
                    if (d.imageUrls.isNotEmpty()) {
                        item { PostImageGrid(urls = d.imageUrls) }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        scope.launch {
                                            CommunityRepository.likePost(d.id)
                                                .onSuccess { r ->
                                                    val nd = d.copy(liked = r.liked, likeCount = r.likeCount)
                                                    detail = nd
                                                    viewModel.applyLikeToPost(d.id, r.liked, r.likeCount)
                                                }
                                                .onFailure {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        it.message ?: "操作失败",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                        }
                                    }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (d.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "点赞",
                                    tint = if (d.liked) GlassTokens.Rose else GlassTokens.SecondaryLabel,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    d.likeCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "${d.commentCount} 条评论",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlassTokens.SecondaryLabel,
                            )
                        }
                    }
                    item { Text("评论区", style = MaterialTheme.typography.titleSmall, color = GlassTokens.Label) }
                    if (d.comments.isEmpty()) {
                        item {
                            Text(
                                "还没有评论，来抢沙发～",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTokens.SecondaryLabel,
                            )
                        }
                    } else {
                        items(d.comments, key = { it.id }) { c ->
                            CommentRow(
                                c = c,
                                isAdmin = isAdmin,
                                onDelete = {
                                    scope.launch {
                                        CommunityRepository.deletePostComment(d.id, c.id)
                                            .onSuccess { refreshDetail() }
                                            .onFailure {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    it.message ?: "删除失败",
                                                    android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                    }
                                },
                            )
                        }
                    }
                }

                // 评论输入
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { if (it.length <= 1000) commentText = it },
                        modifier = Modifier.weight(1f).height(52.dp),
                        placeholder = { Text("说点什么…", color = GlassTokens.SecondaryLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(GlassTokens.RadiusPill),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassTokens.GlassFill,
                            unfocusedContainerColor = GlassTokens.GlassFill,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = GlassTokens.SystemBlue,
                            focusedTextColor = GlassTokens.Label,
                            unfocusedTextColor = GlassTokens.Label,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    val sendEnabled = commentText.isBlank().not() && !sending
                    val sendBrush: Brush = if (sendEnabled) {
                        GlassTokens.GradientButton
                    } else {
                        SolidColor(GlassTokens.TertiaryLabel)
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(sendBrush)
                            .clickable(enabled = sendEnabled) {
                                sending = true
                                scope.launch {
                                    CommunityRepository.commentPost(d.id, commentText.trim())
                                        .onSuccess {
                                            commentText = ""
                                            sending = false
                                            refreshDetail()
                                        }
                                        .onFailure {
                                            sending = false
                                            android.widget.Toast.makeText(
                                                context,
                                                it.message ?: "评论失败",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showReport) {
        ReportSheet(
            title = "举报动态",
            onDismiss = { showReport = false },
            onSubmit = { reason ->
                showReport = false
                scope.launch {
                    SocialRepository.reportPost(postId, reason)
                        .onSuccess {
                            android.widget.Toast.makeText(context, "举报已提交，感谢反馈", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { e ->
                            android.widget.Toast.makeText(context, e.message ?: "举报失败", android.widget.Toast.LENGTH_SHORT).show()
                        }
                }
            },
        )
    }
}

@Composable
private fun CommentRow(
    c: PostCommentItem,
    isAdmin: Boolean = false,
    onDelete: () -> Unit = {},
) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Avatar(url = c.author.avatarUrl, size = 30)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                c.author.displayName ?: "读者",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = GlassTokens.Label,
            )
            Text(
                c.content,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTokens.Label,
                lineHeight = 20.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatRelativeTime(c.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTokens.SecondaryLabel,
                )
                if (isAdmin) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTokens.Rose,
                        modifier = Modifier
                            .clickable { showConfirm = true }
                            .padding(2.dp),
                    )
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    },
                ) { Text("删除", color = GlassTokens.Rose) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("取消") }
            },
            title = { Text("删除评论", color = GlassTokens.Label) },
            text = { Text("确定删除这条评论吗？其楼中楼也会一并删除，且不可恢复。", color = GlassTokens.SecondaryLabel) },
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  状态占位
// ════════════════════════════════════════════════════════════════
@Composable
private fun CommunityEmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Forum, null, Modifier.size(56.dp), tint = GlassTokens.SecondaryLabel)
        Spacer(Modifier.height(16.dp))
        Text("这里还很安静", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label)
        Spacer(Modifier.height(8.dp))
        Text("点击右上角 + 发布第一条动态吧", style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel)
    }
}

@Composable
private fun CommunityErrorState(error: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Forum, null, Modifier.size(56.dp), tint = GlassTokens.SecondaryLabel)
        Spacer(Modifier.height(16.dp))
        Text(error, style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GlassTokens.GradientButton)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text("重试", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CommunityLoginHint(onAccountClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Forum, null, Modifier.size(56.dp), tint = GlassTokens.SecondaryLabel)
        Spacer(Modifier.height(16.dp))
        Text("登录后查看关注动态", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GlassTokens.GradientButton)
                .clickable(onClick = onAccountClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text("去登录", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  工具
// ════════════════════════════════════════════════════════════════
private fun formatRelativeTime(ts: Long): String {
    if (ts <= 0) return ""
    val min = (System.currentTimeMillis() - ts) / 60000
    return when {
        min < 1 -> "刚刚"
        min < 60 -> "${min}分钟前"
        min < 60 * 24 -> "${min / 60}小时前"
        min < 60 * 24 * 30 -> "${min / (60 * 24)}天前"
        else -> "${min / (60 * 24 * 30)}个月前"
    }
}
