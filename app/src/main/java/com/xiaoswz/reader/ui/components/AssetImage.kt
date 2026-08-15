package com.xiaoswz.reader.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image

/**
 * 从 `app/src/main/assets/art/` 加载美术资产。
 *
 * 所有 AI 生成的角色立绘 / 深海背景统一放在 `assets/art/<category>/<file>.png`，
 * 通过 Android [Context.assets] 直接读取（不依赖 Coil 的 URI scheme）。
 *
 * path 只需写相对 `art/` 的部分，例如 `"character/character_full.png"`。
 * 内部自动加上 `"art/"` 前缀后从 assets 打开。
 */
@Composable
fun ArtImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    // 用 remember + key=path 避免每次重组重读文件；路径变了才重新加载
    val imageBitmap: ImageBitmap? = remember(path) {
        runCatching {
            context.assets.open("art/$path").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
    // 图片读取失败时静默不渲染（不留空白或报错）
}
