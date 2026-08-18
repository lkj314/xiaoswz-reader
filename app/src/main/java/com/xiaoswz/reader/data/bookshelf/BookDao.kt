package com.xiaoswz.reader.data.bookshelf

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    /** 书架列表，按最后阅读时间倒序 */
    @Query("SELECT * FROM bookshelf ORDER BY last_read_at DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM bookshelf WHERE slug = :slug")
    suspend fun getBySlug(slug: String): BookEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookshelf WHERE slug = :slug)")
    suspend fun exists(slug: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity)

    @Update
    suspend fun update(book: BookEntity)

    /** 仅更新已存在的记录的阅读进度（不存在则无操作） */
    @Query(
        "UPDATE bookshelf SET last_chapter_id = :chapterId, " +
            "last_chapter_title = :chapterTitle, last_read_at = :ts WHERE slug = :slug",
    )
    suspend fun updateProgress(
        slug: String,
        chapterId: String,
        chapterTitle: String?,
        ts: Long,
    )

    /** 更新阅读进度并同步进度百分比（0.16.0） */
    @Query(
        "UPDATE bookshelf SET last_chapter_id = :chapterId, " +
            "last_chapter_title = :chapterTitle, last_read_at = :ts, " +
            "progress_percent = :progressPercent WHERE slug = :slug",
    )
    suspend fun updateProgressWithPercent(
        slug: String,
        chapterId: String,
        chapterTitle: String?,
        progressPercent: Int,
        ts: Long,
    )

    /** 设置阅读状态；finished 时进度置 100，其余状态保留已有进度（0.16.0） */
    @Query(
        "UPDATE bookshelf SET status = :status, " +
            "progress_percent = :progressPercent WHERE slug = :slug",
    )
    suspend fun updateStatus(
        slug: String,
        status: String,
        progressPercent: Int,
    )

    @Query("DELETE FROM bookshelf WHERE slug = :slug")
    suspend fun deleteBySlug(slug: String)
}
