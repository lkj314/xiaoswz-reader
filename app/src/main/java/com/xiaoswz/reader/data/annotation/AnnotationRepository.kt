package com.xiaoswz.reader.data.annotation

import android.content.Context
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.BOOK_SOURCE_MAIN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 标注仓储：本地文件为权威，云端做跨设备合并（LWW by updatedAt）。
 * 未登录时云端调用失败被 runCatching 吞掉，本地标注照常工作。
 */
object AnnotationRepository {

    fun newClientId(): String = UUID.randomUUID().toString()

    fun createHighlight(
        bookId: String,
        chapterId: String,
        start: Int,
        end: Int,
        quoted: String?,
        color: Int,
    ): AnnotationEntity = AnnotationEntity(
        clientId = newClientId(),
        bookSourceId = BOOK_SOURCE_MAIN,
        bookId = bookId,
        chapterId = chapterId,
        type = ANNOTATION_TYPE_HIGHLIGHT,
        startOffset = minOf(start, end),
        endOffset = maxOf(start, end),
        quotedText = quoted,
        color = color,
        note = null,
        updatedAt = System.currentTimeMillis(),
    )

    fun loadLocal(ctx: Context, bookId: String): List<AnnotationEntity> =
        AnnotationStore.load(ctx, bookId)

    fun persist(ctx: Context, bookId: String, items: List<AnnotationEntity>) {
        AnnotationStore.save(ctx, bookId, items)
    }

    /** 上传单条（新建/更新/删除墓碑）。失败静默。 */
    suspend fun pushOne(ctx: Context, item: AnnotationEntity) {
        runCatching {
            BackendClient.api.pushAnnotations(AnnotationPushBody(listOf(item.toDto())))
        }
    }

    /** 云端拉取 + 本地合并：本地为准，云端补充本地缺失项；本地更新项回传云端。 */
    suspend fun sync(ctx: Context, bookId: String): List<AnnotationEntity> =
        withContext(Dispatchers.IO) {
            val local = loadLocal(ctx, bookId).toMutableList()
            runCatching {
                val remote = BackendClient.api.getAnnotations(bookId).items
                val remoteMap = remote.associateBy { it.clientId }
                // 拉取：补充本地没有的（含云端较新的）
                for (r in remote) {
                    if (local.none { it.clientId == r.clientId }) local.add(r.toEntity())
                }
                // 推送：本地有但云端没有 / 本地更新更新 → 上传
                val toPush = local.filter { l ->
                    val r = remoteMap[l.clientId]
                    r == null || l.updatedAt > r.updatedAt
                }.map { it.toDto() }
                if (toPush.isNotEmpty()) {
                    BackendClient.api.pushAnnotations(AnnotationPushBody(toPush))
                }
                persist(ctx, bookId, local)
            }
            local
        }
}
