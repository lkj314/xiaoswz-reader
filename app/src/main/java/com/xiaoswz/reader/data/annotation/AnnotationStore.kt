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

    /**
     * 读取标注。
     * - 文件不存在 → 空列表（正常的「没有数据」）；
     * - 解析失败 → **null**（表示数据损坏，H3）。
     *
     * 修复（H3）：原先损坏时静默返回空列表，上层拿空列表再 persist 就把整本书的标注
     * 永久清空且零报错。改返回 null，让调用方能区分「没数据」与「损坏」，损坏时跳过 persist。
     */
    fun load(ctx: Context, bookId: String): List<AnnotationEntity>? {
        val f = file(ctx, bookId)
        if (!f.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(AnnotationEntity.serializer()),
                f.readText(),
            )
        }.getOrNull()
    }

    /** 不关心「损坏 vs 无数据」的只读场景：损坏时按空列表处理（不会写回覆盖） */
    fun loadOrEmpty(ctx: Context, bookId: String): List<AnnotationEntity> = load(ctx, bookId) ?: emptyList()

    /**
     * 落盘（原子写）：先写同名 .tmp 再 renameTo 替换。
     *
     * 修复（H3）：原先直接覆盖写，写入过程中被杀进程/磁盘满会把文件截断成半截 JSON，
     * 下次 load 解析失败 → 静默空列表 → 上层覆盖写 → 全部标注永久丢失。
     */
    fun save(ctx: Context, bookId: String, items: List<AnnotationEntity>) {
        runCatching {
            val target = file(ctx, bookId)
            val text = json.encodeToString(ListSerializer(AnnotationEntity.serializer()), items)
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(text)
            // 同一目录下的 renameTo 是原子替换；Windows 上目标已存在时 renameTo 会失败，故先删目标
            if (target.exists() && !target.delete()) {
                tmp.delete()
                return@runCatching
            }
            if (tmp.renameTo(target)) return@runCatching
            // 极少数文件系统不支持原子替换：退化成覆盖写，并清理临时文件
            target.writeText(text)
            tmp.delete()
        }
    }
}
