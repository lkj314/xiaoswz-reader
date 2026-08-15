package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xiaoswz.reader.ui.theme.WhaleColors

/**
 * 全局深海沉浸式背景：在内容之下铺一张 `bg_deep_ocean` 深海图，
 * 再叠一层半透明深海底色保证文字可读性。所有顶层页面套用后，
 * 卡片之间的空隙会透出深海纹理，呈现 ANIBUZZ 式沉浸氛围。
 *
 * 叠加强度 [overlayAlpha] 默认 0.45（优先可读性，小鲸风头不抢戏）。
 */
@Composable
fun WhaleBackground(
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.45f,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        ArtImage(
            path = "background/bg_deep_ocean.png",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleColors.OceanDeep.copy(alpha = overlayAlpha)),
        )
        content()
    }
}
