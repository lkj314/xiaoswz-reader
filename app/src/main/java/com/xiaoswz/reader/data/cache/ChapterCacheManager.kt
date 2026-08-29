package com.xiaoswz.reader.data.cache

import android.util.Base64
import android.util.Log
import com.xiaoswz.reader.data.AppContext
import com.xiaoswz.reader.data.model.ChapterContentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 章节正文离线缓存：每章序列化为 JSON 文件存入应用私有目录。
 *
 * 刻意不进入 Room —— AppDatabase 使用 fallbackToDestructiveMigration()，
 * 一旦改动 schema 会清空整个 bookshelf.db（书架与阅读进度全丢）。
 * 用独立文件目录规避该风险，随应用卸载自动清除。
 *
 * 0.9.1 增强：
 *  - TTL（默认 7 天）：过期章节视为未命中，下次读取自动回源刷新，不再永久陈旧。
 *  - LRU 容量上限（默认 300MB）：写入时若超出上限，按最后修改时间淘汰最旧章节，
 *    避免缓存无限膨胀挤占磁盘。
 *  - isFresh(id)：供预取判断「是否已缓存且新鲜」，命中则跳过网络请求（护主站）。
 */
object ChapterCacheManager {
    private const val DIR_NAME = "chapter_cache"
    private const val TAG = "ChapterCache"
    private val json = Json { ignoreUnknownKeys = true }
    private val ready = AtomicBoolean(false)
    private lateinit var dir: File

    /** 章节正文保鲜期：超过则下次读取回源刷新 */
    private const val TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 天

    /** 缓存总容量上限，超出触发 LRU 淘汰 */
    private const val CAP_BYTES = 300L * 1024 * 1024 // 300 MB

    // ── 淘汰节流 ──
    // evictIfNeeded() 会 listFiles() + sortedBy 全目录扫描，上千文件时单次可达数十毫秒；
    // 原来每次 put() 都跑一遍，连续下载/预取时直接卡死主线程。改为累计 N 次或间隔超时才跑。
    private const val EVICT_EVERY_PUTS = 20
    private const val EVICT_MIN_INTERVAL_MS = 30_000L
    private val putCounter = AtomicInteger(0)

    @Volatile
    private var lastEvictAtMs = System.currentTimeMillis()
    private val evictLock = Any()

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

    private fun isFreshFile(f: File): Boolean {
        if (!f.exists()) return false
        return System.currentTimeMillis() - f.lastModified() <= TTL_MS
    }

    /** 是否已缓存且新鲜（供预取跳过判断，不删除任何文件） */
    fun isFresh(chapterId: String): Boolean {
        ensureDir()
        return isFreshFile(fileFor(chapterId))
    }

    fun get(chapterId: String): ChapterContentDto? {
        ensureDir()
        val f = fileFor(chapterId)
        if (!f.exists()) return null
        // 过期：删除旧文件并视为未命中，触发回源刷新
        if (!isFreshFile(f)) {
            f.delete()
            return null
        }
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
            val f = fileFor(id)
            f.writeText(json.encodeToString(ChapterContentDto.serializer(), dto))
            // 以最后修改时间记录写入时刻，供 TTL / LRU 使用
            f.setLastModified(System.currentTimeMillis())
            // 修复：淘汰改为节流触发，不再每次写入都全目录扫描
            maybeEvict()
        } catch (e: Exception) {
            Log.w(TAG, "write failed: $id", e)
        }
    }

    /** 节流版淘汰：累计 EVICT_EVERY_PUTS 次写入或距上次淘汰超过 EVICT_MIN_INTERVAL_MS 才真正执行 */
    private fun maybeEvict() {
        val n = putCounter.incrementAndGet()
        val now = System.currentTimeMillis()
        val due = n >= EVICT_EVERY_PUTS || now - lastEvictAtMs >= EVICT_MIN_INTERVAL_MS
        if (!due) return
        synchronized(evictLock) {
            // 双重检查，避免并发 put 同时触发多次全目录扫描
            val nowIn = System.currentTimeMillis()
            if (putCounter.get() < EVICT_EVERY_PUTS && nowIn - lastEvictAtMs < EVICT_MIN_INTERVAL_MS) return
            putCounter.set(0)
            lastEvictAtMs = nowIn
            evictIfNeeded()
        }
    }

    /** 超过容量上限时，按最后修改时间淘汰最旧章节，直到回到上限内 */
    private fun evictIfNeeded() {
        ensureDir()
        val files = dir.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= CAP_BYTES) return
        // 最旧在前
        val sorted = files.sortedBy { it.lastModified() }
        for (f in sorted) {
            if (total <= CAP_BYTES) break
            total -= f.length()
            f.delete()
        }
    }

    /** 清理所有过期章节（可在设置页或冷启动时调用，保持缓存整洁） */
    fun pruneStale() {
        ensureDir()
        dir.listFiles()?.forEach { f ->
            if (!isFreshFile(f)) f.delete()
        }
    }

    fun contains(chapterId: String): Boolean = isFresh(chapterId)

    fun entryCount(): Int {
        ensureDir()
        return dir.listFiles()?.size ?: 0
    }

    fun sizeBytes(): Long {
        ensureDir()
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clear() {
        ensureDir()
        dir.listFiles()?.forEach { it.delete() }
    }

    // ─────────────────────────────────────────────────────────────
    // suspend 版本：内部切到 Dispatchers.IO 执行同步文件读写。
    // 不改原同步方法签名（历史调用点多），新增版本供协程调用点使用，避免主线程阻塞/ANR。
    // ─────────────────────────────────────────────────────────────

    suspend fun getSuspend(chapterId: String): ChapterContentDto? =
        withContext(Dispatchers.IO) { get(chapterId) }

    suspend fun putSuspend(dto: ChapterContentDto) =
        withContext(Dispatchers.IO) { put(dto) }

    suspend fun isFreshSuspend(chapterId: String): Boolean =
        withContext(Dispatchers.IO) { isFresh(chapterId) }

    suspend fun containsSuspend(chapterId: String): Boolean = isFreshSuspend(chapterId)

    suspend fun pruneStaleSuspend() =
        withContext(Dispatchers.IO) { pruneStale() }

    /** 强制立即执行一次容量淘汰（如下载完成后调用，绕过节流） */
    suspend fun evictNowSuspend() =
        withContext(Dispatchers.IO) { synchronized(evictLock) { evictIfNeeded() } }

    suspend fun entryCountSuspend(): Int =
        withContext(Dispatchers.IO) { entryCount() }

    suspend fun sizeBytesSuspend(): Long =
        withContext(Dispatchers.IO) { sizeBytes() }

    suspend fun clearSuspend() =
        withContext(Dispatchers.IO) { clear() }

    /** 批量判断「哪些 id 已缓存且新鲜」：一次 IO 调度算完，供目录列表等批量场景避免逐项文件 IO */
    suspend fun freshIdsSuspend(ids: Collection<String>): Set<String> =
        withContext(Dispatchers.IO) { ids.filterTo(HashSet<String>()) { isFresh(it) } }
}
