package com.xiaoswz.reader.data.booklist

import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.BooklistCreateBody
import com.xiaoswz.reader.data.api.BooklistItemBody
import com.xiaoswz.reader.data.api.BooklistListResponse
import com.xiaoswz.reader.data.api.BooklistDetail
import com.xiaoswz.reader.data.api.CollectResponse
import com.xiaoswz.reader.data.api.HomeResponse
import com.xiaoswz.reader.data.api.HotBooklistsResponse

/**
 * 书单推书（0.7.4）+ 首页运营位（0.7.6）。统一 Result<T> 包裹，映射社区门禁错误。
 */
object BooklistRepository {
    private fun <T> mapError(e: Throwable): Result<T> {
        val msg = e.message ?: ""
        return when {
            msg.contains("login_required") -> Result.failure(Exception("请先登录后再操作"))
            msg.contains("forbidden") -> Result.failure(Exception("无权操作"))
            msg.contains("booklist not found") -> Result.failure(Exception("书单不存在"))
            else -> Result.failure(e)
        }
    }

    suspend fun getBooklists(scope: String, page: Int): Result<BooklistListResponse> = runCatching {
        BackendClient.api.getBooklists(scope, page)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun createBooklist(title: String, description: String?, coverUrl: String?): Result<String> =
        runCatching {
            BackendClient.api.createBooklist(
                BooklistCreateBody(title = title, description = description, coverUrl = coverUrl),
            ).id
        }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getDetail(id: String): Result<BooklistDetail> = runCatching {
        BackendClient.api.getBooklistDetail(id)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun addItem(
        id: String,
        bookSourceId: String,
        bookId: String,
        title: String?,
        author: String?,
        coverUrl: String?,
        note: String?,
        bookUid: String? = null,
    ): Result<Boolean> = runCatching {
        BackendClient.api.addBooklistItem(
            id,
            BooklistItemBody(
                bookSourceId = bookSourceId,
                bookId = bookId,
                bookUid = bookUid,
                title = title,
                author = author,
                coverUrl = coverUrl,
                note = note,
            ),
        ).ok
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun deleteItem(id: String, itemId: String): Result<Boolean> = runCatching {
        BackendClient.api.deleteBooklistItem(id, itemId).ok
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun collect(id: String): Result<CollectResponse> = runCatching {
        BackendClient.api.collectBooklist(id)
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getHotBooklists(): Result<HotBooklistsResponse> = runCatching {
        BackendClient.api.getHotBooklists()
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })

    suspend fun getHome(): Result<HomeResponse> = runCatching {
        BackendClient.api.getHome()
    }.fold(onSuccess = { Result.success(it) }, onFailure = { mapError(it) })
}
