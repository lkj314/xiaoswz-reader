package com.xiaoswz.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xiaoswz.reader.ui.theme.WhaleColors

/**
 * 全局 Meta 背景：在内容之下铺一层极浅灰白渐变（白 → #F6F7F9），
 * 干净无彩、无光斑，呈现 Meta 白底灰边的扁平质感。纯代码绘制，不使用任何图片素材。
 */
@Composable
fun WhaleBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhaleColors.GradientBackground),
        )
        content()
    }
}
