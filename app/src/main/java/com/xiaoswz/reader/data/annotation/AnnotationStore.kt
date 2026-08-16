package com.xiaoswz.reader.data.annotation

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 标注本地持久化：每本书一个 JSON 文件（files/annotations/<bookId>.json）。
 * 不碰 Room schema（物理隔离 + 避免破坏性迁移），走文件存储。
 */
object AnnotationStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(ctx: Context, bookId: String): File {
        val dir = File(ctx.filesDir, "annotations")
        dir.mkdirs()
        return File(dir, "$bookId.json")
    }

    fun load(ctx: Context, bookId: String): List<AnnotationEntity> {
        val f = file(ctx, bookId)
        if (!f.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(AnnotationEntity.serializer()),
                f.readText(),
            )
        }.getOrElse { emptyList() }
    }

    fun save(ctx: Context, bookId: String, items: List<AnnotationEntity>) {
        runCatching {
            file(ctx, bookId).writeText(
                json.encodeToString(ListSerializer(AnnotationEntity.serializer()), items),
            )
        }
    }
}
