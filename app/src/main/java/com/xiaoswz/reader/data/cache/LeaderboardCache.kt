package com.xiaoswz.reader.data.cache

import android.util.Base64
import android.util.Log
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.api.LeaderboardResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 榜单离线缓存（0.9.0）：把后端排行榜快照序列化到应用私有目录。
 * 书库页的「热榜 / 月票榜」横滑行渲染时优先读此处，做到**打开即显示上一次的榜单**，
 * 再后台静默刷新——彻底消除「进书库要等榜单加载」的延迟。
 * 不进 Room（规避 AppDatabase.fallbackToDestructiveMigration 风险），独立文件目录。
 */
object LeaderboardCache {
    private const val DIR_NAME = "leaderboard_cache"
    private const val TAG = "LeaderboardCache"
    private const val TTL_MS = 30L * 60 * 1000 // 30min 内视为新鲜
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var dir: File

    @Serializable
    private data class Entry(val savedAt: Long, val resp: LeaderboardResponse)

    private fun ensureDir() {
        if (ready.get()) return
        synchronized(this) {
            if (ready.get()) return
            dir = File(AppContext.app.filesDir, DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            ready.set(true)
        }
    }

    private fun fileFor(board: String): File {
        val safe = Base64.encodeToString(
            board.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return File(dir, "$safe.json")
    }

    fun isFresh(board: String): Boolean {
        ensureDir()
        val f = fileFor(board)
        if (!f.exists()) return false
        return try {
            val entry = json.decodeFromString(Entry.serializer(), f.readText())
            System.currentTimeMillis() - entry.savedAt < TTL_MS
        } catch (e: Exception) {
            false
        }
    }

    fun get(board: String): LeaderboardResponse? {
        ensureDir()
        val f = fileFor(board)
        if (!f.exists()) return null
        return try {
            json.decodeFromString(Entry.serializer(), f.readText()).resp
        } catch (e: Exception) {
            Log.w(TAG, "read failed: $board", e)
            null
        }
    }

    fun put(board: String, resp: LeaderboardResponse) {
        ensureDir()
        try {
            fileFor(board).writeText(
                json.encodeToString(Entry.serializer(), Entry(System.currentTimeMillis(), resp)),
            )
        } catch (e: Exception) {
            Log.w(TAG, "write failed: $board", e)
        }
    }

    fun clear() {
        ensureDir()
        dir.listFiles()?.forEach { it.delete() }
    }
}
