package com.xiaoswz.reader.data.cache

import android.util.Base64
import android.util.Log
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.model.ChapterContentDto
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 章节正文离线缓存：每章序列化为 JSON 文件存入应用私有目录。
 *
 * 刻意不进入 Room —— AppDatabase 使用 fallbackToDestructiveMigration()，
 * 一旦改动 schema 会清空整个 bookshelf.db（书架与阅读进度全丢）。
 * 用独立文件目录规避该风险，随应用卸载自动清除。
 */
object ChapterCacheManager {
    private const val DIR_NAME = "chapter_cache"
    private const val TAG = "ChapterCache"
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var dir: File

    private fun ensureDir() {
        if (ready.get()) return
        synchronized(this) {
            if (ready.get()) return
            dir = File(AppContext.app.filesDir, DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            ready.set(true)
        }
    }

    /** 用 URL-Safe Base64 编码 chapterId 作为文件名，避免特殊字符且保证唯一 */
    private fun fileFor(chapterId: String): File {
        val safe = Base64.encodeToString(
            chapterId.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return File(dir, "$safe.json")
    }

    fun get(chapterId: String): ChapterContentDto? {
        ensureDir()
        val f = fileFor(chapterId)
        if (!f.exists()) return null
        return try {
            json.decodeFromString(ChapterContentDto.serializer(), f.readText())
        } catch (e: Exception) {
            Log.w(TAG, "read failed: $chapterId", e)
            null
        }
    }

    fun put(dto: ChapterContentDto) {
        val id = dto.id ?: return
        ensureDir()
        try {
            fileFor(id).writeText(json.encodeToString(ChapterContentDto.serializer(), dto))
        } catch (e: Exception) {
            Log.w(TAG, "write failed: $id", e)
        }
    }

    fun contains(chapterId: String): Boolean {
        ensureDir()
        return fileFor(chapterId).exists()
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
