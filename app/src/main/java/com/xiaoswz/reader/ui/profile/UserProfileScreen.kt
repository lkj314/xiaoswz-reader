package com.xiaoswz.reader.ui.profile
import com.xiaoswz.reader.ui.components.MetaCoverImage
import com.xiaoswz.reader.ui.components.MetaAvatarImage

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.data.api.PostItem
import com.xiaoswz.reader.data.api.UserBookshelfItem
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.xiaoswz.reader.ui.theme.MetaIcons

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onBooklistClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: UserProfileViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isSelf by remember { mutableStateOf(false) }
    var justBlocked by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        isSelf = try {
            AppSettingsRepository(context.applicationContext).getAccountId() == userId
        } catch (_: Exception) { false }
        viewModel.load(userId)
    }

    Scaffold(
        topBar = { AppTopBar(title = "个人主页", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        if (state.isLoading && state.profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassTokens.SystemBlue)
            }
            return@Scaffold
        }
        val profile = state.profile
        if (profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "用户不存在", color = GlassTokens.SecondaryLabel)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = profile.avatarUrl, size = 64, name = profile.displayName ?: "")
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.displayName ?: "读者", style = MaterialTheme.typography.titleLarge, color = GlassTokens.Label, fontWeight = FontWeight.Bold)
                            val role = profile.role
                            if (role != null && (role == "admin" || role == "official")) {
                                Spacer(Modifier.width(6.dp))
                                Text(role, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier
                                    .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                                    .background(GlassTokens.SystemBlue)
                                    .padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatChip("${profile.followerCount}", "粉丝")
                            StatChip("${profile.followingCount}", "关注")
                            StatChip("${profile.postCount}", "动态")
                            StatChip("${profile.booklistCount}", "书单")
                        }
                    }
                }
            }

            // 阅读激励（0.7.7）：等级 / 连续天数 / 累计时长
            val st = profile.stats
            if (st != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
                            .background(GlassTokens.GlassFillStrong)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatChip("Lv.${st.level}", "等级")
                        StatChip("${st.streakDays}", "连续天")
                        StatChip("${st.days}", "打卡天")
                        StatChip("${st.totalMin / 60}h", "阅读")
                    }
                }
            }

            // 关注 / 拉黑（非自己才显示）
            if (!isSelf) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val followLabel = if (profile.isFollowing) "已关注" else "关注"
                        val followBg = if (profile.isFollowing)
                            Modifier.background(GlassTokens.GlassFillStrong)
                        else
                            Modifier.background(GlassTokens.GradientButton)
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                                .then(followBg)
                                .clickable {
                                    scope.launch {
                                        viewModel.toggleFollow(userId) { res ->
                                            res.onFailure { e ->
                                                android.widget.Toast.makeText(context, e.message ?: "操作失败", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MetaIcons.PersonAdd, null, tint = if (profile.isFollowing) GlassTokens.Label else Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(followLabel, color = if (profile.isFollowing) GlassTokens.Label else Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(GlassTokens.RadiusMD))
                                .background(GlassTokens.GlassFillStrong)
                                .clickable {
                                    scope.launch {
                                        viewModel.block(userId) { res ->
                                            res.onSuccess { b ->
                                                justBlocked = true
                                                android.widget.Toast.makeText(context, if (b) "已拉黑该用户" else "已取消拉黑", android.widget.Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                android.widget.Toast.makeText(context, e.message ?: "操作失败", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MetaIcons.Block, null, tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("拉黑", color = GlassTokens.Label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 标签页
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.tab == ProfileTab.POSTS, onClick = { viewModel.switchTab(userId, ProfileTab.POSTS) }, label = { Text("动态") })
                    FilterChip(selected = state.tab == ProfileTab.BOOKLISTS, onClick = { viewModel.switchTab(userId, ProfileTab.BOOKLISTS) }, label = { Text("书单") })
                    FilterChip(selected = state.tab == ProfileTab.BOOKSHELF, onClick = { viewModel.switchTab(userId, ProfileTab.BOOKSHELF) }, label = { Text("书架") })
                }
            }

            when (state.tab) {
                ProfileTab.POSTS -> {
                    if (state.posts.isEmpty()) {
                        item { EmptyHint("还没有发布动态") }
                    } else {
                        items(state.posts, key = { it.id }) { post ->
                            ProfilePostRow(post = post)
                        }
                    }
                }
                ProfileTab.BOOKLISTS -> {
                    if (state.booklists.isEmpty()) {
                        item { EmptyHint("还没有创建书单") }
                    } else {
                        items(state.booklists, key = { it.id }) { bl ->
                            ProfileBooklistRow(bl = bl, onClick = { onBooklistClick(bl.id) })
                        }
                    }
                }
                ProfileTab.BOOKSHELF -> {
                    if (state.bookshelf.isEmpty()) {
                        item { EmptyHint("书架为空或未公开") }
                    } else {
                        items(state.bookshelf, key = { "${it.bookSourceId}:${it.bookId}" }) { item ->
                            ProfileBookshelfRow(item = item, onClick = { onBookClick(item.bookId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
    }
}

@Composable
private fun Avatar(url: String?, size: Int, name: String = "") {
    MetaAvatarImage(
        model = url,
        name = name.ifBlank { "我" },
        modifier = Modifier.size(size.dp),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.SecondaryLabel)
    }
}

@Composable
private fun ProfilePostRow(post: PostItem) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .padding(14.dp),
    ) {
        Text(post.content, style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, lineHeight = 22.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
        if (post.imageUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                post.imageUrls.take(3).forEach { url ->
                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(MetaIcons.Favorite, null, tint = GlassTokens.SecondaryLabel, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${post.likeCount}", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
            Spacer(Modifier.width(14.dp))
            Text("${post.commentCount} 评论", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
        }
    }
}

@Composable
private fun ProfileBooklistRow(bl: BooklistSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            MetaCoverImage(
                model = bl.coverUrl,
                title = bl.title,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(GlassTokens.RadiusMD),
                cornerRadius = GlassTokens.RadiusMD,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bl.title, style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${bl.collectCount} 收藏 · ${bl.itemCount} 本", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
        }
    }
}

@Composable
private fun ProfileBookshelfRow(item: UserBookshelfItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            MetaCoverImage(
                model = item.coverUrl,
                title = item.title ?: "未知书目",
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(GlassTokens.RadiusMD),
                cornerRadius = GlassTokens.RadiusMD,
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title ?: "未知书目", style = MaterialTheme.typography.bodyLarge, color = GlassTokens.Label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.author?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
