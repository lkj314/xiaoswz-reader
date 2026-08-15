package com.xiaoswz.reader.data.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.xiaoswz.reader.BuildConfig
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable

/**
 * 数据模型 - 对应冲浪中文网公开 API 的 JSON 结构
 * 注意：所有字段均可空（服务端可能不返回某些字段）
 */

// ─────────────────────────────────────────────
// GET /api/books（书城列表，分页）
// ─────────────────────────────────────────────

@Serializable
data class BookListResponse(
    val books: List<BookDto>? = null,
    val total: Int? = null,
    val page: Int? = null,
    val totalPages: Int? = null,
)

@Serializable
data class BookDto(
    val id: String? = null,
    val uid: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val status: String? = null,
    val wordCount: Int? = null,
    val chapterCount: Int? = null,
    val viewCount: Int? = null,
    val displayHeat: Double? = null,
    val authorName: String? = null,
    val updatedAt: String? = null,
    val author: AuthorDto? = null,
    val category: CategoryDto? = null,
) {
    /** 显示用作者名：优先笔名 */
    val displayAuthor: String
        get() = author?.penName?.takeIf { it.isNotBlank() }
            ?: author?.name?.takeIf { it.isNotBlank() }
            ?: authorName?.takeIf { it.isNotBlank() }
            ?: "佚名"

    /** 连载状态中文 */
    val statusText: String
        get() = when (status) {
            "ONGOING" -> "连载中"
            "COMPLETED" -> "已完结"
            "HIATUS" -> "暂停中"
            "DROPPED" -> "已切书"
            else -> ""
        }
}

@Serializable
data class AuthorDto(
    val name: String? = null,
    val penName: String? = null,
)

@Serializable
data class CategoryDto(
    val name: String? = null,
    val slug: String? = null,
)

// ─────────────────────────────────────────────
// GET /api/book-source?action=detail（书籍详情 + 目录）
// ─────────────────────────────────────────────

@Serializable
data class BookDetailDto(
    val id: String? = null,
    val name: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
    val wordCount: Int? = null,
    val status: String? = null,
    val chapterCount: Int? = null,
    val chapters: List<ChapterDto>? = null,
)

@Serializable
data class ChapterDto(
    val id: String? = null,
    val name: String? = null,
    val wordCount: Int? = null,
    val url: String? = null,
    val index: Int? = null,
)

// ─────────────────────────────────────────────
// GET /api/book-source?action=content（章节正文）
// ─────────────────────────────────────────────

@Serializable
data class ChapterContentDto(
    val id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val bookName: String? = null,
    val bookSlug: String? = null,
)

// ─────────────────────────────────────────────
// 通用格式化
// ─────────────────────────────────────────────

/** 字数格式化：12345 → "1.2万字" */
fun formatWordCount(count: Int?): String {
    val c = count ?: return ""
    return if (c >= 10000) {
        val wan = c / 10000f
        if (wan >= 100) "${wan.toInt()}万字" else "%.1f万字".format(wan)
    } else {
        "${c}字"
    }
}

/**
 * 把封面字段解析成 Coil 能直接加载的对象。
 *
 * 真机文件日志（cover_debug.txt）已确认：主站封面返回的是
 * `data:image/jpeg;base64,...` 这种**绝对 data URI**，URL 拼接本身没问题。
 * 真正导致封面不显示的根因：**Coil 的 AsyncImage 把 String 类型的 data URI 当成
 * 普通 URL 去解析，没有对应的 fetcher，于是静默加载失败**（文字正常、图全空）。
 * 网页 <img> 与阅读3.0 能直接显示 data URI，但 Coil 不行。
 *
 * 规则：
 * - 空 / 空白 → 返回 null（交给 Coil 显示占位）
 * - data: URI → base64 解码成 ByteBuffer，交给 Coil 的 ByteBufferFetcher
 * - 已是 http(s) → 原样返回
 * - 协议相对路径 //host/x → 补 https: 协议
 * - 其他相对路径 → 拼上 API_BASE_URL(origin)
 */
fun resolveCoverUrl(raw: String?): Any? {
    if (raw.isNullOrBlank()) {
        logCoverToFile(raw, "null")
        return null
    }
    val trimmed = raw.trim()
    val resolved: Any? = when {
        // data: URI —— 必须解码成字节，否则 Coil 无法加载（这是封面不显示的真正原因）
        trimmed.startsWith("data:", ignoreCase = true) -> decodeDataUri(trimmed)
        trimmed.startsWith("http://", ignoreCase = true)
            || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        // 协议相对路径（如 //cdn.xxx.com/x.jpg）：浏览器/阅读3.0 会补 https: 协议
        trimmed.startsWith("//") -> "https:$trimmed"
        else -> {
            val origin = BuildConfig.API_BASE_URL.trimEnd('/')
            if (trimmed.startsWith("/")) "$origin$trimmed" else "$origin/$trimmed"
        }
    }
    val desc = when {
        resolved == null -> "null"
        resolved is ByteBuffer -> "ByteBuffer(${resolved.remaining()}B)"
        resolved is ByteArray -> "ByteArray(${resolved.size}B)"
        else -> (resolved as String).take(120)
    }
    logCoverToFile(raw, desc)
    return resolved
}

/** data: URI 解码：仅支持 base64，输出 ByteBuffer 供 Coil 的 ByteBufferFetcher 加载 */
private fun decodeDataUri(uri: String): Any? {
    return try {
        val comma = uri.indexOf(',')
        if (comma < 0) return uri
        val meta = uri.substring(5, comma) // 去掉 "data:" 前缀
        val payload = uri.substring(comma + 1)
        if (meta.contains(";base64", ignoreCase = true)) {
            ByteBuffer.wrap(Base64.decode(payload, Base64.DEFAULT))
        } else {
            uri // 非 base64（如 urlencoded）暂回退原样，不崩溃
        }
    } catch (_: Throwable) {
        uri
    }
}

/**
 * 把封面 data URI 压缩成小缩略图 data URI（宽度 <= 320px，JPEG q80）。
 *
 * 用途：书架把封面存进 Room 的 cover_url 列。若直接存主站原始 data URI（几百 KB~几 MB），
 * 单行会超过 SQLite CursorWindow 上限（约 1~2MB），导致书架查询抛
 * SQLiteBlobTooBigException 整体崩溃。压缩成缩略图后单行仅几 KB~几十 KB，远离上限，
 * 离线也能显示（resolveCoverUrl 已支持 data: -> ByteBuffer）。
 *
 * 非 data: 的远程 URL 原样返回（本身就小）。解码/压缩失败返回 null（显示占位，不崩）。
 */
fun shrinkCover(dataUri: String?): String? {
    if (dataUri.isNullOrBlank()) return null
    if (!dataUri.startsWith("data:", ignoreCase = true)) return dataUri
    return try {
        val comma = dataUri.indexOf(',')
        if (comma < 0) return dataUri
        val meta = dataUri.substring(5, comma) // 去掉 "data:" 前缀
        if (!meta.contains(";base64", ignoreCase = true)) return dataUri
        val payload = dataUri.substring(comma + 1)
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val maxW = 320
        val scale = (maxW.toFloat() / bmp.width.coerceAtLeast(1)).coerceAtMost(1f)
        val w = (bmp.width * scale).toInt().coerceAtLeast(1)
        val h = (bmp.height * scale).toInt().coerceAtLeast(1)
        val thumb = Bitmap.createScaledBitmap(bmp, w, h, true)
        bmp.recycle()
        val out = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
        thumb.recycle()
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (_: Throwable) {
        null
    }
}

/**
 * 调试用：把封面解析结果写入 App 内部文件（vivo 等 ROM 会屏蔽第三方 App 的 logcat，
 * 故改用文件落地，再用 adb run-as 拉取）。仅调试，不影响正常逻辑。
 */
private fun logCoverToFile(raw: String?, resolvedDesc: String?) {
    try {
        val at = Class.forName("android.app.ActivityThread")
        val ctx = at.getMethod("currentApplication").invoke(null) as? android.content.Context
        ctx?.let {
            val f = java.io.File(it.filesDir, "cover_debug.txt")
            val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            f.appendText("[$ts] raw=[${(raw ?: "null")?.take(120)}] resolved=[${resolvedDesc ?: "null"}]\n")
        }
    } catch (_: Throwable) {
        // 调试日志失败不影响封面加载
    }
}
