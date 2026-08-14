package com.xiaoswz.reader.data

import android.content.Context

/**
 * 应用上下文持有者：在 MainActivity.onCreate 初始化一次，
 * 供无 Context 的仓储层（文件缓存、更新标记）获取 filesDir 使用。
 */
object AppContext {
    lateinit var app: Context
        private set

    fun init(context: Context) {
        if (!::app.isInitialized) {
            app = context.applicationContext
        }
    }
}
