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
import com.xiaoswz.reader.data.api.SegmentCommentBody
import com.xiaoswz.reader.data.api.SegmentCommentItem
import com.xiaoswz.reader.data.api.SegmentCommentListResponse
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
import com.xiaoswz.reader.data.api.AdminBookListResponse
import com.xiaoswz.reader.data.api.AdminBookPatchResponse
import com.xiaoswz.reader.data.api.AdminCharacterListResponse
import com.xiaoswz.reader.data.api.AdminCharacterUpsertResponse
import com.xiaoswz.reader.data.api.AdminAnnouncementListResponse
import com.xiaoswz.reader.data.api.AdminAnnouncementResponse

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

    // ── v0.15 章评 / 段评 ──
    /** 章评列表（锚定到章节） */
    suspend fun getChapterComments(bookId: String, chapterId: String, page: Int = 1): Result<CommentListResponse> = runCatching {
        api.getChapterComments(BOOK_SOURCE_MAIN, bookId, chapterId, page)
    }

    /** 发章评（登录） */
    suspend fun postChapterComment(bookId: String, chapterId: String, content: String, parentId: String? = null): Result<Unit> = runCatching {
        api.postChapterComment(BOOK_SOURCE_MAIN, bookId, chapterId, CommentBody(content, parentId))
    }

    /** 段评列表（按章节一次性拉取） */
    suspend fun getSegmentComments(bookId: String, chapterId: String): Result<SegmentCommentListResponse> = runCatching {
        api.getSegmentComments(BOOK_SOURCE_MAIN, bookId, chapterId)
    }

    /** 发段评（登录）：paragraphIndex 必填，偏移相对该段 trim 正文，quotedText 引用快照 */
    suspend fun postSegmentComment(
        bookId: String,
        chapterId: String,
        content: String,
        paragraphIndex: Int,
        startOffset: Int?,
        endOffset: Int?,
        quotedText: String?,
        parentId: String? = null,
    ): Result<SegmentCommentItem> = runCatching {
        api.postSegmentComment(
            BOOK_SOURCE_MAIN,
            bookId,
            chapterId,
            SegmentCommentBody(content, parentId, paragraphIndex, startOffset, endOffset, quotedText),
        )
    }

    // ── 创作者 / 管理（admin，App 内嵌模块）──
    /** 管理台：书籍列表（q 书名过滤） */
    suspend fun adminListBooks(q: String?, page: Int): Result<AdminBookListResponse> = runCatching {
        api.adminListBooks(q, page)
    }

    /** 管理台：书籍元数据编辑（书名/作者/封面/隐藏） */
    suspend fun adminPatchBook(
        src: String,
        id: String,
        title: String?,
        author: String?,
        coverUrl: String?,
        hidden: Boolean?,
    ): Result<AdminBookPatchResponse> = runCatching {
        api.adminPatchBook(src, id, com.xiaoswz.reader.data.api.AdminBookPatchBody(title, author, coverUrl, hidden))
    }

    /** 管理台：某书角色列表（按 bookId 过滤） */
    suspend fun adminListCharacters(bookId: String): Result<AdminCharacterListResponse> = runCatching {
        api.adminListCharacters(bookId)
    }

    /** 管理台：角色录入 / 更新（同名则更新） */
    suspend fun adminUpsertCharacter(
        bookId: String,
        name: String,
        roleType: String,
        avatarUrl: String?,
        description: String?,
        order: Int,
    ): Result<AdminCharacterUpsertResponse> = runCatching {
        api.adminUpsertCharacter(
            com.xiaoswz.reader.data.api.AdminCharacterUpsertBody(
                bookId = bookId,
                name = name,
                roleType = roleType,
                avatarUrl = avatarUrl,
                description = description,
                order = order,
            ),
        )
    }

    /** 管理台：删除角色 */
    suspend fun adminDeleteCharacter(id: String): Result<Unit> = runCatching {
        api.adminDeleteCharacter(id)
    }

    /** 管理台：公告列表 */
    suspend fun adminListAnnouncements(): Result<AdminAnnouncementListResponse> = runCatching {
        api.adminListAnnouncements()
    }

    /** 管理台：新建公告 */
    suspend fun adminCreateAnnouncement(
        title: String,
        body: String,
        level: String,
    ): Result<AdminAnnouncementResponse> = runCatching {
        api.adminCreateAnnouncement(com.xiaoswz.reader.data.api.AdminAnnouncementBody(title, body, level))
    }

    /** 管理台：编辑公告 */
    suspend fun adminPatchAnnouncement(
        id: String,
        title: String,
        body: String,
        level: String,
    ): Result<AdminAnnouncementResponse> = runCatching {
        api.adminPatchAnnouncement(id, com.xiaoswz.reader.data.api.AdminAnnouncementBody(title, body, level))
    }

    /** 管理台：删除公告 */
    suspend fun adminDeleteAnnouncement(id: String): Result<Unit> = runCatching {
        api.adminDeleteAnnouncement(id)
    }
}
