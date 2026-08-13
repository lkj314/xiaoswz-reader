package com.xiaoswz.reader.data.model

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
