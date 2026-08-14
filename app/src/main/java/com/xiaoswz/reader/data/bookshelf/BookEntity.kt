package com.xiaoswz.reader.data.bookshelf

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地书架实体：收藏的书籍 + 最后阅读进度。
 * slug 即书籍唯一标识（与网站书源 slug 一致）。
 */
@Entity(tableName = "bookshelf")
data class BookEntity(
    @PrimaryKey
    val slug: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,

    /** 首次加入书架时记录的第一章 id，保证续读永远有入口 */
    @ColumnInfo(name = "first_chapter_id")
    val firstChapterId: String?,

    /** 最后阅读章节 id（每次进阅读器自动更新） */
    @ColumnInfo(name = "last_chapter_id")
    val lastChapterId: String?,

    /** 最后阅读章节标题（仅展示用） */
    @ColumnInfo(name = "last_chapter_title")
    val lastChapterTitle: String?,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long,
)
