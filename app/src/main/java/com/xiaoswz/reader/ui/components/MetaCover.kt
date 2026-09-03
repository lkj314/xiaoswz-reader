package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.xiaoswz.reader.data.model.resolveCoverUrl
import com.xiaoswz.reader.ui.theme.GlassTokens
import com.xiaoswz.reader.ui.theme.MetaCoverPalette
import com.xiaoswz.reader.ui.theme.WhaleColors

/**
 * Meta 排版式书封占位（0.20.0）
 *
 * 封面缺失 / 加载中 / 加载失败时显示。不再是纯灰空框，而是：
 * 柔彩渐变底（由书名哈希确定性取色）+ 钴蓝短强调条 + 大字号书名 + 作者 + 发丝内描边。
 *
 * 同一本书在任何界面色彩恒定，不会出现列表刷新时封面颜色跳变。
 */
@Composable
fun MetaCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    author: String? = null,
    /** 取色种子，默认书名 */
    seed: String = title,
    cornerRadius: Dp = GlassTokens.RadiusXL,
    hairline: Boolean = true,
) {
    val palette = remember(seed) { MetaCoverPalette.of(seed) }
    val shape = RoundedCornerShape(cornerRadius)
    val hairlinePx = with(LocalDensity.current) { 1.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(brush = MetaCoverPalette.brush(palette, size.width, size.height))
                if (hairline) {
                    val inset = hairlinePx / 2f
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.07f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - hairlinePx, size.height - hairlinePx),
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        style = Stroke(width = hairlinePx),
                    )
                }
            },
    ) {
        val maxW = maxWidth
        val maxH = maxHeight
        val pad = maxW * 0.10f
        val titleSize = (maxW.value * 0.185f).sp
        val authorSize = (maxW.value * 0.105f).sp

        // 首字水印：极低透明度的大号字形，给柔彩底一点编排感
        val initial = title.trim().firstOrNull()
        if (initial != null && maxW.value >= 56f) {
            Text(
                text = initial.toString(),
                fontSize = (maxW.value * 0.95f).sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink.copy(alpha = 0.07f),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = maxW * 0.10f, y = maxH * 0.14f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pad, vertical = maxW * 0.11f),
        ) {
            // 钴蓝短强调条
            Box(
                modifier = Modifier
                    .size(width = maxW * 0.20f, height = maxOf(2.dp, maxW * 0.028f))
                    .clip(CircleShape)
                    .background(WhaleColors.OceanMid),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = title,
                fontSize = titleSize,
                lineHeight = titleSize * 1.2f,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!author.isNullOrBlank()) {
                Spacer(Modifier.height(maxW * 0.045f))
                Text(
                    text = author,
                    fontSize = authorSize,
                    lineHeight = authorSize * 1.3f,
                    color = palette.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Meta 书封图片：加载中 / 失败 / 无封面时统一回落到排版式柔彩占位。
 *
 * model 传封面原始串（http / data: / 相对路径均可），内部走 [resolveCoverUrl] 归一化。
 */
@Composable
fun MetaCoverImage(
    model: String?,
    title: String,
    modifier: Modifier = Modifier,
    author: String? = null,
    contentDescription: String? = title,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusXL),
    cornerRadius: Dp = GlassTokens.RadiusXL,
) {
    val resolved = remember(model) { resolveCoverUrl(model) }
    SubcomposeAsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = { MetaCoverPlaceholder(title, Modifier.fillMaxSize(), author, cornerRadius = cornerRadius) },
        error = { MetaCoverPlaceholder(title, Modifier.fillMaxSize(), author, cornerRadius = cornerRadius) },
    )
}

/**
 * Meta 头像占位：柔彩底 + 首字。用于用户头像 / 角色头像缺省态。
 */
@Composable
fun MetaAvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    seed: String = name,
    shape: Shape = CircleShape,
) {
    val palette = remember(seed) { MetaCoverPalette.of(seed) }
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(MetaCoverPalette.flat(palette)),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name.trim().firstOrNull()?.toString().orEmpty()
        if (initial.isNotEmpty()) {
            Text(
                text = initial,
                fontSize = (maxWidth.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.ink.copy(alpha = 0.78f),
                maxLines = 1,
            )
        }
    }
}

/**
 * Meta 头像图片：加载中 / 失败时回落到柔彩首字占位。
 */
@Composable
fun MetaAvatarImage(
    model: String?,
    name: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = name,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = CircleShape,
) {
    val resolved = remember(model) { resolveCoverUrl(model) }
    SubcomposeAsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = { MetaAvatarPlaceholder(name, Modifier.fillMaxSize(), shape = shape) },
        error = { MetaAvatarPlaceholder(name, Modifier.fillMaxSize(), shape = shape) },
    )
}

/**
 * 书籍封面标准比例容器（2:3），供书架 / 书城 / 书单复用。
 */
@Composable
fun MetaCoverFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.aspectRatio(2f / 3f)) { content() }
}
