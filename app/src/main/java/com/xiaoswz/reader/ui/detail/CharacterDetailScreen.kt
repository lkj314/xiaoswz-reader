@file:OptIn(ExperimentalLayoutApi::class)

package com.xiaoswz.reader.ui.detail
import com.xiaoswz.reader.ui.components.MetaAvatarImage

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xiaoswz.reader.data.api.CharacterTagDto
import com.xiaoswz.reader.data.settings.AppSettingsRepository
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.components.LiquidGlassCard
import com.xiaoswz.reader.ui.components.SectionHeader
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.MetaButton
import com.xiaoswz.reader.ui.components.MetaButtonVariant
import com.xiaoswz.reader.ui.theme.MetaIcons

private fun roleLabel(roleType: String?): String = when (roleType) {
    "main" -> "主角"
    "supporting" -> "配角"
    else -> roleType ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: String,
    onBack: () -> Unit,
    onAccountClick: () -> Unit = {},
    viewModel: CharacterDetailViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appSettings = remember { AppSettingsRepository(context.applicationContext) }
    val loggedIn by appSettings.isLoggedInFlow.collectAsState(initial = false)
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(characterId) {
        viewModel.load(characterId)
    }

    // 后端反馈轻量 Toast
    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.character?.name ?: "角色",
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
                    MetaButton(text = "重试", onClick = { viewModel.load(characterId) })
                }
            }

            else -> {
                val ch = state.character ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    // ── 角色信息头部 ──
                    item {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            radius = GlassTokens.RadiusLG,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                MetaAvatarImage(
                                    model = ch.avatarUrl,
                                    name = ch.name,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (roleLabel(ch.roleType).isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = roleLabel(ch.roleType),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GlassTokens.SecondaryLabel,
                                    )
                                }
                                if (!ch.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = ch.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                // 比心按钮
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    IconButton(onClick = { viewModel.toggleHeart() }) {
                                        Icon(
                                            imageVector = if (ch.myHeart) MetaIcons.Favorite else MetaIcons.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (ch.myHeart) Color(0xFFE25555) else GlassTokens.SecondaryLabel,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                    Text(
                                        text = "${ch.heartCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = GlassTokens.Label,
                                    )
                                    Text(
                                        text = if (ch.myHeart) "已比心" else "比心",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GlassTokens.SecondaryLabel,
                                    )
                                }
                            }
                        }
                    }

                    // ── 标签墙 ──
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader(title = "标签墙")
                            Spacer(Modifier.height(10.dp))
                            if (ch.tags.isEmpty()) {
                                Text(
                                    text = "还没有标签，来做第一个贴标签的人吧",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GlassTokens.SecondaryLabel,
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ch.tags.forEach { tag ->
                                        TagChip(tag, onClick = { viewModel.toggleTagVote(tag.id) })
                                    }
                                }
                            }
                            // 登录用户可新建标签
                            if (loggedIn) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = state.newTagName,
                                        onValueChange = { viewModel.onNewTagNameChange(it) },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("给 TA 贴个标签…", color = GlassTokens.SecondaryLabel) },
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
                                        modifier = Modifier,
                                        onClick = { viewModel.createTag(state.newTagName) },
                                        enabled = state.newTagName.trim().isNotEmpty() && !state.postingTag,
                                    ) {
                                        if (state.postingTag) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = LocalContentColor.current,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text("贴上")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 角色讨论 ──
                    item {
                        HorizontalDivider()
                        SectionHeader(title = "讨论（${state.commentTotal}）")
                    }
                    items(
                        items = state.comments,
                        key = { it.id },
                    ) { c ->
                        CharacterCommentRow(
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
                                    modifier = Modifier,
                                    onClick = {
                                        viewModel.postComment(commentText)
                                        commentText = ""
                                    },
                                    enabled = commentText.isNotBlank(),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                MetaButton(
                                    text = "登录后参与讨论 ›",
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
}

/** 单个标签气泡：点击投票；已投高亮 */
@Composable
private fun TagChip(
    tag: CharacterTagDto,
    onClick: () -> Unit,
) {
    val selected = tag.myVote
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassTokens.RadiusPill))
            .background(
                if (selected) GlassTokens.SystemBlue.copy(alpha = 0.15f) else GlassTokens.GlassFillStrong,
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, GlassTokens.SystemBlue, RoundedCornerShape(GlassTokens.RadiusPill))
                } else {
                    Modifier
                },
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTokens.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "▲ ${tag.voteCount}",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.SecondaryLabel,
            )
        }
    }
}

/** 单条角色讨论（与书籍评论同风格） */
@Composable
private fun CharacterCommentRow(
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
