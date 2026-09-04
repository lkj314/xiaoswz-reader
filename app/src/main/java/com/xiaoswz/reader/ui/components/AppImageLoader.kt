package com.xiaoswz.reader.ui.components

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * 全局单例 Coil [ImageLoader]（0.20.4 性能修复）。
 *
 * 背景：0.20.0 重写 MetaCover 时只用了裸 `SubcomposeAsyncImage(model = ...)`，
 * 全仓没有任何 ImageLoader 配置 —— 既没有内存缓存也没有磁盘缓存，
 * 于是「每次打开首页 / 书城 / 书架 / 书籍详情，封面都要重新联网下载 + 重新解码」，
 * 表现就是用户反馈的「需要缓存很长一段时间」。
 *
 * 这里补上稳定的双层缓存：
 * - 内存缓存：可用堆的 25%，列表来回滑动 / 页面重进时立刻命中，不再闪白；
 * - 磁盘缓存：50MB，落在 cacheDir/image_cache，冷启动也能直接命中，不再重新下载。
 *
 * 注意：`data:image/...` 这类内嵌封面被解成 ByteBuffer 模型，Coil 不对其做磁盘缓存
 * （无稳定缓存键）。但这部分封面本来就存在本地数据库里、体积已被压缩到 56KB 以下，
 * 解码开销极小，不是瓶颈；真正的瓶颈是占绝大多数的 http 封面，正是本缓存的目标。
 */
object AppImageLoader {

    private const val DISK_CACHE_BYTES = 50L * 1024 * 1024 // 50MB

    @Volatile
    private var instance: ImageLoader? = null

    /** 进程内唯一 ImageLoader；重复调用直接复用，不会重复建缓存目录。 */
    fun get(context: Context): ImageLoader = instance ?: synchronized(this) {
        instance ?: build(context.applicationContext).also { instance = it }
    }

    private fun build(appContext: Context): ImageLoader = ImageLoader.Builder(appContext)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
}
