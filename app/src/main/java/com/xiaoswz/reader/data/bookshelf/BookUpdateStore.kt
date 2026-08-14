package com.xiaoswz.reader.data.bookshelf

import com.xiaoswz.reader.data.AppContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 书籍章节更新状态：记录每本书"已知章节数"与"有更新"标记。
 * 存为应用私有 JSON 文件，不进 Room（避免清空书架风险）。
 *
 * 数据流：
 * - 加入书架时记录已知章节数（setKnown）
 * - 打开书籍时比对服务端章节数，更大则标"有更新"（markUpdated）
 * - 用户打开阅读后清除标记（markSeen / clearUpdate）
 */
@Serializable
private data class UpdateStoreData(
    val known: Map<String, Int> = emptyMap(),
    val hasUpdate: Map<String, Boolean> = emptyMap(),
)

object BookUpdateStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var file: File
    private var data: UpdateStoreData = UpdateStoreData()

    private fun ensure() {
        if (ready.get()) return
        synchronized(this) {
            if (ready.get()) return
            file = File(AppContext.app.filesDir, "book_update.json")
            data = if (file.exists()) {
                try {
                    json.decodeFromString(UpdateStoreData.serializer(), file.readText())
                } catch (e: Exception) {
                    UpdateStoreData()
                }
            } else {
                UpdateStoreData()
            }
            ready.set(true)
        }
    }

    private fun persist() {
        try {
            file.writeText(json.encodeToString(UpdateStoreData.serializer(), data))
        } catch (e: Exception) {
            // 持久化失败不影响内存态，下次写入会重试
        }
    }

    fun getKnown(slug: String): Int? {
        ensure()
        return data.known[slug]
    }

    fun getHasUpdate(slug: String): Boolean {
        ensure()
        return data.hasUpdate[slug] ?: false
    }

    /** 记录当前已知章节数（加入书架 / 打开书籍时调用） */
    fun setKnown(slug: String, count: Int) {
        ensure()
        data = data.copy(known = data.known + (slug to count))
        persist()
    }

    /** 标记有更新（详情页检测到服务端章节数 > 已知时） */
    fun markUpdated(slug: String) {
        ensure()
        data = data.copy(hasUpdate = data.hasUpdate + (slug to true))
        persist()
    }

    /** 打开书籍后清除"有更新"标记并记录当前章节数 */
    fun markSeen(slug: String, currentCount: Int) {
        ensure()
        data = data.copy(
            hasUpdate = data.hasUpdate + (slug to false),
            known = data.known + (slug to currentCount),
        )
        persist()
    }

    /** 仅清除"有更新"标记（书架点击打开时），不动已知章节数 */
    fun clearUpdate(slug: String) {
        ensure()
        data = data.copy(hasUpdate = data.hasUpdate + (slug to false))
        persist()
    }
}
