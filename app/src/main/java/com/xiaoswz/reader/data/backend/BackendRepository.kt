package com.xiaoswz.reader.data.backend

import com.xiaoswz.reader.data.api.AdCreativeDto
import com.xiaoswz.reader.data.api.AdClickBody
import com.xiaoswz.reader.data.api.AdImpressionBody
import com.xiaoswz.reader.data.api.AdResponse
import com.xiaoswz.reader.data.api.AttributionBody
import com.xiaoswz.reader.data.api.BackendClient
import com.xiaoswz.reader.data.api.BookStatsResponse
import com.xiaoswz.reader.data.api.BOOK_SOURCE_MAIN
import com.xiaoswz.reader.data.api.CommentBody
import com.xiaoswz.reader.data.api.CommentItem
import com.xiaoswz.reader.data.api.CommentListResponse
import com.xiaoswz.reader.data.api.LeaderboardResponse
import com.xiaoswz.reader.data.api.RatingResponse
import com.xiaoswz.reader.data.api.VoteBalance
import com.xiaoswz.reader.data.api.VoteBody
import com.xiaoswz.reader.data.api.VoteResult
import com.xiaoswz.reader.data.api.CharacterListResponse
import com.xiaoswz.reader.data.api.CharacterDetailResponse
import com.xiaoswz.reader.data.api.HeartResponse
import com.xiaoswz.reader.data.api.TagListResponse
import com.xiaoswz.reader.data.api.TagResponse
import com.xiaoswz.reader.data.api.TagBody
import com.xiaoswz.reader.data.api.TagVoteResponse

/**
 * 冲浪阅读独立后端的统一访问层。所有方法都包了 runCatching——
 * 后端未启动 / 局域网不可达 / 非 2xx 都安全降级为失败 Result，绝不抛到 UI 线程。
 * 书源统一为 [BOOK_SOURCE_MAIN]（主站），不接任何外部源。
 */
object BackendRepository {

    private val api get() = BackendClient.api

    suspend fun reportBookView(
        bookId: String,
        title: String?,
        author: String?,
        coverUrl: String?,
    ): Result<Unit> = runCatching {
        api.reportBookView(BOOK_SOURCE_MAIN, bookId, com.xiaoswz.reader.data.api.BookViewBody(title, author, coverUrl))
    }

    suspend fun getBookStats(bookId: String): Result<BookStatsResponse> = runCatching {
        api.getBookStats(BOOK_SOURCE_MAIN, bookId)
    }

    suspend fun getLeaderboard(board: String): Result<LeaderboardResponse> = runCatching {
        api.getLeaderboard(board)
    }

    suspend fun getVoteBalance(): Result<VoteBalance> = runCatching {
        api.getVoteBalance()
    }

    suspend fun castVote(bookId: String, voteType: String = "monthly"): Result<VoteResult> = runCatching {
        api.castVote(VoteBody(bookId = bookId, voteType = voteType))
    }

    suspend fun getRating(bookId: String): Result<RatingResponse> = runCatching {
        api.getRating(BOOK_SOURCE_MAIN, bookId)
    }

    suspend fun submitRating(bookId: String, score: Int): Result<Unit> = runCatching {
        api.submitRating(com.xiaoswz.reader.data.api.RatingBody(bookId = bookId, score = score))
    }

    suspend fun getComments(bookId: String, page: Int = 1): Result<CommentListResponse> = runCatching {
        api.getComments(BOOK_SOURCE_MAIN, bookId, page)
    }

    suspend fun postComment(bookId: String, content: String, parentId: String? = null): Result<Unit> = runCatching {
        api.postComment(BOOK_SOURCE_MAIN, bookId, CommentBody(content, parentId))
    }

    suspend fun likeComment(id: String): Result<Unit> = runCatching {
        api.likeComment(id)
    }

    suspend fun reportComment(id: String): Result<Unit> = runCatching {
        api.reportComment(id)
    }

    suspend fun getAds(slot: String): Result<AdResponse> = runCatching {
        api.getAds(slot)
    }

    suspend fun reportImpression(creativeId: String?, slot: String): Result<Unit> = runCatching {
        api.reportImpression(AdImpressionBody(creativeId, slot))
    }

    suspend fun reportClick(impressionId: String?): Result<Unit> = runCatching {
        api.reportClick(AdClickBody(impressionId))
    }

    suspend fun reportAttribution(channel: String?, referrer: String? = null, campaign: String? = null): Result<Unit> = runCatching {
        api.reportAttribution(AttributionBody(channel, referrer, campaign))
    }

    // ── 0.14.0 书籍角色互动 ──
    /** 书籍角色列表（横滑卡片） */
    suspend fun getCharacters(bookId: String): Result<CharacterListResponse> = runCatching {
        api.getCharacters(BOOK_SOURCE_MAIN, bookId)
    }

    /** 角色详情：比心数 + 我的比心 + 标签墙 */
    suspend fun getCharacterDetail(characterId: String): Result<CharacterDetailResponse> = runCatching {
        api.getCharacterDetail(characterId)
    }

    /** 比心切换（登录） */
    suspend fun toggleHeart(characterId: String): Result<HeartResponse> = runCatching {
        api.toggleHeart(characterId)
    }

    /** 角色标签列表 */
    suspend fun getTags(characterId: String): Result<TagListResponse> = runCatching {
        api.getTags(characterId)
    }

    /** 新建角色标签（登录） */
    suspend fun createTag(characterId: String, name: String): Result<TagResponse> = runCatching {
        api.createTag(characterId, TagBody(name))
    }

    /** 标签投票切换（登录） */
    suspend fun toggleTagVote(tagId: String): Result<TagVoteResponse> = runCatching {
        api.toggleTagVote(tagId)
    }

    /** 角色讨论列表（复用 Comment 表） */
    suspend fun getCharacterComments(characterId: String, page: Int = 1): Result<CommentListResponse> = runCatching {
        api.getCharacterComments(characterId, page)
    }

    /** 发角色讨论（登录） */
    suspend fun postCharacterComment(characterId: String, content: String, parentId: String? = null): Result<Unit> = runCatching {
        api.postCharacterComment(characterId, CommentBody(content, parentId))
    }
}
