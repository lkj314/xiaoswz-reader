package com.xiaoswz.reader.data.cache

import android.util.Base64
import android.util.Log
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.model.BookListResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 书城首屏目录离线缓存（0.8.0）：按 sort（latest/popular）缓存第一页列表。
 * 开书城秒开，30min 内视为新鲜；与 BookMetaCache 同理，独立文件目录、不进 Room。
 */
object CatalogCache {
    private const val DIR_NAME = "catalog_cache"
    private const val TAG = "CatalogCache"
    private const val TTL_MS = 30L * 60 * 1000
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var dir: File

    @Serializable
    private data class Entry(val savedAt: Long, val resp: BookListResponse)

    private fun ensureDir() {
        if (ready.get()) return
        synchronized(this) {
            if (ready.get()) return
            dir = File(AppContext.app.filesDir, DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            ready.set(true)
        }
    }

    private fun fileFor(sort: String): File {
        val safe = Base64.encodeToString(
            sort.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP,
        )
        return File(dir, "$safe.json")
    }

    fun isFresh(sort: String): Boolean {
        ensureDir()
        val f = fileFor(sort)
        if (!f.exists()) return false
        return try {
            val entry = json.decodeFromString(Entry.serializer(), f.readText())
            System.currentTimeMillis() - entry.savedAt < TTL_MS
        } catch (e: Exception) {
            false
        }
    }

    fun get(sort: String): BookListResponse? {
        ensureDir()
        val f = fileFor(sort)
        if (!f.exists()) return null
        return try {
            json.decodeFromString(Entry.serializer(), f.readText()).resp
        } catch (e: Exception) {
            Log.w(TAG, "read failed: $sort", e)
            null
        }
    }

    fun put(sort: String, resp: BookListResponse) {
        ensureDir()
        try {
            fileFor(sort).writeText(
                json.encodeToString(Entry.serializer(), Entry(System.currentTimeMillis(), resp)),
            )
        } catch (e: Exception) {
            Log.w(TAG, "write failed: $sort", e)
        }
    }

    fun clear() {
        ensureDir()
        dir.listFiles()?.forEach { it.delete() }
    }
}
