package com.xiaoswz.reader.data.cache

import android.util.Base64
import android.util.Log
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.model.BookDetailDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 书籍元数据离线缓存（0.8.0）：把书籍详情 + 目录序列化到应用私有目录。
 *
 * 解决「每次启动/登录都重新拉书源」——detailCache 原本只是内存缓存，进程死就丢。
 * 这里用独立文件目录（同 ChapterCacheManager 思路），不进 Room，
 * 规避 AppDatabase.fallbackToDestructiveMigration() 改 schema 清空书架的风险。
 */
object BookMetaCache {
    private const val DIR_NAME = "book_meta"
    private const val TAG = "BookMetaCache"
    private const val TTL_MS = 24L * 3600 * 1000 // 24h：书源目录一天内视为新鲜
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var dir: File

    @Serializable
    private data class Entry(val savedAt: Long, val dto: BookDetailDto)

    private fun ensureDir() {
        if (ready.get()) return
        synchronized(this) {
            if (ready.get()) return
            dir = File(AppContext.app.filesDir, DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            ready.set(true)
        }
    }

    private fun fileFor(slug: String): File {
        val safe = Base64.encodeToString(
            slug.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return File(dir, "$safe.json")
    }

    fun isFresh(slug: String): Boolean {
        ensureDir()
        val f = fileFor(slug)
        if (!f.exists()) return false
        return try {
            val entry = json.decodeFromString(Entry.serializer(), f.readText())
            System.currentTimeMillis() - entry.savedAt < TTL_MS
        } catch (e: Exception) {
            false
        }
    }

    fun get(slug: String): BookDetailDto? {
        ensureDir()
        val f = fileFor(slug)
        if (!f.exists()) return null
        return try {
            json.decodeFromString(Entry.serializer(), f.readText()).dto
        } catch (e: Exception) {
            Log.w(TAG, "read failed: $slug", e)
            null
        }
    }

    fun put(slug: String, dto: BookDetailDto) {
        ensureDir()
        try {
            fileFor(slug).writeText(
                json.encodeToString(Entry.serializer(), Entry(System.currentTimeMillis(), dto)),
            )
        } catch (e: Exception) {
            Log.w(TAG, "write failed: $slug", e)
        }
    }

    fun sizeBytes(): Long {
        ensureDir()
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clear() {
        ensureDir()
        dir.listFiles()?.forEach { it.delete() }
    }
}
