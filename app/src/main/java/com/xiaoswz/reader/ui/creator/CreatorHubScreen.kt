package com.xiaoswz.reader.ui.creator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.ui.components.AppTopBar
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.components.whaleGlassCard

@Composable
fun CreatorHubScreen(
    onBack: () -> Unit,
    onCharacters: () -> Unit,
    onBooks: () -> Unit,
    onAnnouncements: () -> Unit,
    onAuthorLogs: () -> Unit,
) {
    Scaffold(
        topBar = { AppTopBar(title = "创作者中心", onBack = onBack, showLogo = false) },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "管理功能已内嵌到 App，无需再开独立后台网页。以下操作以管理员账号身份执行。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTokens.SecondaryLabel,
                )
            }
            item {
                HubCard(
                    icon = Icons.Default.Person,
                    title = "角色录入",
                    subtitle = "为书籍新增 / 编辑 / 删除热门角色（名字、主角配角、头像、简介）",
                    onClick = onCharacters,
                )
            }
            item {
                HubCard(
                    icon = Icons.Default.Edit,
                    title = "书籍编辑",
                    subtitle = "搜索书籍，修改书名 / 作者 / 封面 / 隐藏状态（不碰正文）",
                    onClick = onBooks,
                )
            }
            item {
                HubCard(
                    icon = Icons.Default.Campaign,
                    title = "公告管理",
                    subtitle = "发布 / 编辑 / 删除运营公告（信息 / 警告 / 重要）",
                    onClick = onAnnouncements,
                )
            }
            item {
                HubCard(
                    icon = Icons.Default.Chat,
                    title = "作者日志",
                    subtitle = "写碎碎念 / 公告 / 章节改动说明，读者在书籍详情页可见",
                    onClick = onAuthorLogs,
                )
            }
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .whaleGlassCard()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = GlassTokens.SystemBlue, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.Label,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTokens.SecondaryLabel,
                )
            }
        }
    }
}
