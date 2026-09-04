package com.xiaoswz.reader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.xiaoswz.reader.ui.components.AppImageLoader

/**
 * 应用入口（0.20.4 性能修复）。
 *
 * 唯一职责：实现 Coil 的 [ImageLoaderFactory]，把带双层缓存的 ImageLoader 设为**进程内单例**。
 *
 * 为什么不放 Compose 层（LocalImageLoader）：Coil 2.x 已废弃 `LocalImageLoader.provide` ——
 * 它**不会**设置单例，一旦混用 `LocalImageLoader.current` 与 `context.imageLoader`，
 * 就会悄悄创建出第二个没有缓存的 ImageLoader，缓存等于白配。
 * 实现 ImageLoaderFactory 后，所有 `AsyncImage` / `SubcomposeAsyncImage` /
 * `rememberAsyncImagePainter` 都自动拿到这一个实例，Compose 内外都命中同一份缓存。
 */
class SurfReaderApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = AppImageLoader.get(this)
}
