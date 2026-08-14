package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoswz.reader.R

/**
 * 全站统一顶部栏（v0.3.9 抽出，根因见 TOPBAR-POSTMORTEM.md）：
 *
 * - 背景从屏幕顶部（y=0）起，状态栏图标直接叠加其上，绝不为状态栏预留任何纯色块。
 * - 栏高 = 内容高度（logo / 标题 / 按键的实际高度），零额外垂直 padding、零 statusBarsPadding。
 * - 左侧：返回键（onBack != null 时）或海浪 logo（showLogo 时）。根页面放品牌 logo，子页面放返回键。
 * - 右侧：actions 自定义（如书城的更新图标）。
 *
 * 设计约束（用户铁律）：字号 / 图标多高，栏就多高，绝不为「状态栏 / 规范」预留一分。
 */
@Composable
fun AppTopBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    showLogo: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val color = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = color,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(6.dp)
                        .size(24.dp),
                )
                if (showLogo) Spacer(Modifier.width(4.dp))
            }
            if (showLogo) {
                Image(
                    painter = painterResource(R.drawable.ic_surf_logo),
                    contentDescription = "冲浪阅读",
                    colorFilter = ColorFilter.tint(color, BlendMode.SrcIn),
                    modifier = Modifier.size(28.dp),
                )
                if (title != null) Spacer(Modifier.width(8.dp))
            }
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}
