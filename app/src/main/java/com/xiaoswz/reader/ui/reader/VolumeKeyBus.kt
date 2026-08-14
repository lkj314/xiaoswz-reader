package com.xiaoswz.reader.ui.reader

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 音量键事件总线
 * MainActivity 拦截音量键 → 阅读器按需消费
 */
object VolumeKeyBus {
    /** 阅读器是否在前台 */
    @Volatile
    var readerActive: Boolean = false

    /** 设置里是否启用了音量键翻页 */
    @Volatile
    var pagingEnabled: Boolean = true

    val events = MutableSharedFlow<Int>(extraBufferCapacity = 8)
}
