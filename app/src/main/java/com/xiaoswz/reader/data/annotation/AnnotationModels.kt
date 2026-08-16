package com.xiaoswz.reader.data.annotation

import com.xiaoswz.reader.data.api.BOOK_SOURCE_MAIN
import kotlinx.serialization.Serializable

const val ANNOTATION_TYPE_HIGHLIGHT = "highlight"
const val ANNOTATION_TYPE_BOOKMARK = "bookmark"

/**
 * 本地持久化的标注实体（高亮 + 书签统一存储）。
 * - clientId：客户端生成的稳定 UUID，作为云端 upsert / 删除的唯一依据（不因本地重建而改变）。
 * - 字符偏移相对「预处理后正文」(preprocessContent 输出)，与渲染同源。
 * - 不存正文；quotedText 仅作引用快照，便于列表/离线展示。
 */
@Serializable
data class AnnotationEntity(
    val clientId: String,
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val chapterId: String,
    val type: String,
    val startOffset: Int,
    val endOffset: Int,
    val quotedText: String?,
    val color: Int?,
    val note: String?,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

/** 上传 / 拉取用 DTO（与后端字段对齐） */
@Serializable
data class AnnotationDto(
    val clientId: String,
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val chapterId: String,
    val type: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val quotedText: String? = null,
    val color: Int? = null,
    val note: String? = null,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

@Serializable
data class AnnotationPushBody(val items: List<AnnotationDto>)

@Serializable
data class AnnotationListResponse(val items: List<AnnotationDto> = emptyList())

fun AnnotationEntity.toDto(): AnnotationDto = AnnotationDto(
    clientId = clientId,
    bookSourceId = bookSourceId,
    bookId = bookId,
    chapterId = chapterId,
    type = type,
    startOffset = startOffset,
    endOffset = endOffset,
    quotedText = quotedText,
    color = color,
    note = note,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun AnnotationDto.toEntity(): AnnotationEntity = AnnotationEntity(
    clientId = clientId,
    bookSourceId = bookSourceId,
    bookId = bookId,
    chapterId = chapterId,
    type = type,
    startOffset = startOffset,
    endOffset = endOffset,
    quotedText = quotedText,
    color = color,
    note = note,
    updatedAt = updatedAt,
    deleted = deleted,
)
