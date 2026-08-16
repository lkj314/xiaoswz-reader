package com.xiaoswz.reader.ui.community

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.AsyncImage
import com.xiaoswz.reader.data.api.BooklistSummary
import com.xiaoswz.reader.data.api.HomeResponse
import com.xiaoswz.reader.data.booklist.BooklistRepository
import com.xiaoswz.reader.ui.theme.GlassTokens
import kotlinx.coroutines.launch

/**
 * 首页运营位（0.7.6）：横幅 Banner + 公告 + 精选书单。由后端 AppConfig KV（home_banner / featured_booklists）
 * 与 Announcement 表驱动，管理员在 Web 后台配置后实时生效。
 */
@Composable
fun HomeSection(onBooklistClick: (String) -> Unit) {
    val context = LocalContext.current
    var home by remember { mutableStateOf<HomeResponse?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            BooklistRepository.getHome().onSuccess { home = it }
        }
    }

    val data = home ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 横幅（取第一个有效 banner）
        data.banner?.firstOrNull { !it.imageUrl.isNullOrBlank() }?.let { banner ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(GlassTokens.RadiusLG))
                    .background(GlassTokens.GlassFill)
                    .clickable {
                        banner.targetUrl?.takeIf { it.startsWith("http", ignoreCase = true) }?.let { url ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }
                    },
            ) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // 公告
        if (data.announcements.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(GlassTokens.RadiusLG))
                    .background(GlassTokens.GlassFillStrong)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                data.announcements.forEach { ann ->
                    Column {
                        Text(ann.title, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                        ann.body?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = GlassTokens.SecondaryLabel, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // 精选书单
        if (data.featuredBooklists.isNotEmpty()) {
            Column {
                Text("精选书单", style = MaterialTheme.typography.titleMedium, color = GlassTokens.Label, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(data.featuredBooklists, key = { it.id }) { bl ->
                        FeaturedBooklistCard(bl = bl, onClick = { onBooklistClick(bl.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedBooklistCard(bl: BooklistSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(140.dp)
            .clip(RoundedCornerShape(GlassTokens.RadiusLG))
            .background(GlassTokens.GlassFillStrong)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(GlassTokens.RadiusMD)).background(GlassTokens.GlassFill),
            contentAlignment = Alignment.Center,
        ) {
            if (!bl.coverUrl.isNullOrEmpty()) {
                AsyncImage(model = bl.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(GlassTokens.RadiusMD)), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MenuBook, null, tint = GlassTokens.TertiaryLabel, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(bl.title, style = MaterialTheme.typography.bodyMedium, color = GlassTokens.Label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${bl.collectCount} 收藏", style = MaterialTheme.typography.labelSmall, color = GlassTokens.SecondaryLabel)
    }
}
