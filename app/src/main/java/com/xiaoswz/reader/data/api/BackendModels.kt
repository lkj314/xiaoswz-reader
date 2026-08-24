package com.xiaoswz.reader.data.api

import kotlinx.serialization.Serializable

// 所有后端 DTO（请求/响应体）集中定义。
// 从原 BackendApi.kt 拆分而出（P2 文件拆分）：包名保持一致，域接口文件可直接引用，无需额外 import。
// 字段与序列化注解逐字保留，绝不改动，以免影响 JSON 契约。

// ── 请求体 ──
@Serializable
data class DeviceIdBody(val deviceId: String)

@Serializable
data class BookshelfUpsertBody(
    val bookSourceId: String,
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val status: String = "reading",
    val updatedAt: Long,
)

@Serializable
data class ProgressUpsertBody(
    val bookSourceId: String,
    val bookId: String,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val progressPercent: Int = 0,
    val updatedAt: Long,
)

@Serializable
data class BookIdBody(val bookSourceId: String, val bookId: String)

// ── 响应体 ──
@Serializable
data class BackendUser(val id: String, val deviceId: String? = null)

@Serializable
data class AnonResponse(val token: String = "", val user: BackendUser)

// ── 登录账号（user / admin）──
@Serializable
data class AuthRegisterBody(val email: String, val password: String, val deviceId: String? = null)

@Serializable
data class AuthLoginBody(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: AuthUser)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    val role: String = "guest",
    val displayName: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class AuthMeResponse(
    val id: String,
    val email: String? = null,
    val role: String = "guest",
    val displayName: String? = null,
    val deviceId: String? = null,
    val mutedUntil: Long? = null,
)

@Serializable
data class SsoExchangeResponse(val ok: Boolean = false, val sso: String = "", val ttlSec: Int = 0)

@Serializable
data class BookshelfDto(
    val bookSourceId: String,
    val bookId: String,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val status: String = "reading",
    val updatedAt: Long,
    // 软删墓碑标记（0.16.4 起）：true 表示该书被某端删除，本地需同步清除。
    val deleted: Boolean = false,
)

@Serializable
data class ProgressDto(
    val bookSourceId: String,
    val bookId: String,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val progressPercent: Int = 0,
    val updatedAt: Long,
)

@Serializable
data class SyncResponse(
    val serverTime: Long = 0,
    val bookshelf: List<BookshelfDto> = emptyList(),
    val progress: List<ProgressDto> = emptyList(),
)

/** 写入类接口的通用回执；后端额外返回的 item 字段被 ignoreUnknownKeys 忽略 */
@Serializable
data class UpsertAck(val applied: Boolean = true, val reason: String? = null)

@Serializable
data class OkAck(val ok: Boolean = true)

// 书源常量：当前唯一书源 = 主站。绝不接外部源（铁律）。
const val BOOK_SOURCE_MAIN = "main"

// ── P1 书库聚合 ──
@Serializable
data class BookViewBody(
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class BookStatsResponse(
    val found: Boolean = false,
    val bookSourceId: String? = null,
    val bookId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val voteCount: Long = 0,
    val voteMonth: Int = 0,
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Long = 0,
    val favoriteCount: Int = 0,
)

@Serializable
data class LeaderboardResponse(
    val board: String? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
)

@Serializable
data class LeaderboardEntry(
    val bookSourceId: String,
    val bookId: String,
    val title: String? = null,
    /** 绝对 http(s) 封面，可缓存；主站 data: 在响应里会走 coverDataUri 而不是这个字段 */
    val coverUrl: String? = null,
    /** 主站内嵌 data:image/...;base64 封面的临时回退（不入 book_stats，仅响应级） */
    val coverDataUri: String? = null,
    val metric: Double = 0.0,
)

// ── P2 月票 / 评分 / 评论 ──
@Serializable
data class VoteBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val voteType: String = "monthly", // monthly / recommend
)

@Serializable
data class VoteResult(
    val ok: Boolean = true,
    val reason: String? = null,
    val remaining: Int = 0,
)

@Serializable
data class VoteBalance(
    val dailyLimit: Int = 5,
    val used: Int = 0,
    val remaining: Int = 5,
)

@Serializable
data class RatingBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val score: Int,
)

@Serializable
data class RatingResponse(
    val avg: Double = 0.0,
    val count: Int = 0,
    val myScore: Int = 0,
)

@Serializable
data class CommentListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val comments: List<CommentItem> = emptyList(),
)

@Serializable
data class CommentItem(
    val id: String,
    val parentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = 0,
)

@Serializable
data class CommentBody(
    val content: String,
    val parentId: String? = null,
)

// ── v0.15 章评 / 段评 ──
/** 发段评请求体：paragraphIndex 必填（Raw 坐标），start/endOffset 相对该段 trim 后正文，quotedText 为引用快照 */
@Serializable
data class SegmentCommentBody(
    val content: String,
    val parentId: String? = null,
    val paragraphIndex: Int,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
    val quotedText: String? = null,
)

/** 段评条目：在 CommentItem 基础上携带段锚字段 */
@Serializable
data class SegmentCommentItem(
    val id: String,
    val parentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = 0,
    val paragraphIndex: Int? = null,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
    val quotedText: String? = null,
)

/** 段评列表（按章节一次性返回，客户端按 paragraphIndex 分组） */
@Serializable
data class SegmentCommentListResponse(
    val comments: List<SegmentCommentItem> = emptyList(),
)

/** 全书聚合段评条目（v0.15.3 段评独立全屏页）：根段评 + 楼中楼数，跨章节 */
@Serializable
data class BookSegmentCommentItem(
    val id: String,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = 0,
    val chapterId: String? = null,
    val paragraphIndex: Int? = null,
    val quotedText: String? = null,
    val replyCount: Int = 0,
)

/** 全书段评聚合响应（cursor 分页） */
@Serializable
data class BookSegmentCommentListResponse(
    val comments: List<BookSegmentCommentItem> = emptyList(),
    val nextCursor: Long? = null,
    val hasMore: Boolean = false,
)

// ── 0.14.0 书籍角色互动 ──
@Serializable
data class CharacterDto(
    val id: String,
    val name: String,
    val roleType: String = "main", // main / supporting
    val avatarUrl: String? = null,
    val description: String? = null,
    val order: Int = 0,
    val heartCount: Int = 0,
)

@Serializable
data class CharacterListResponse(
    val characters: List<CharacterDto> = emptyList(),
)

@Serializable
data class CharacterTagDto(
    val id: String,
    val name: String,
    val voteCount: Int = 0,
    val myVote: Boolean = false,
)

@Serializable
data class CharacterDetailResponse(
    val id: String,
    val name: String,
    val roleType: String = "main",
    val avatarUrl: String? = null,
    val description: String? = null,
    val heartCount: Int = 0,
    val myHeart: Boolean = false,
    val tags: List<CharacterTagDto> = emptyList(),
)

@Serializable
data class TagListResponse(
    val tags: List<CharacterTagDto> = emptyList(),
)

@Serializable
data class TagResponse(
    val id: String,
    val name: String,
    val voteCount: Int = 0,
    val myVote: Boolean = false,
)

@Serializable
data class TagBody(val name: String)

@Serializable
data class HeartResponse(
    val hearted: Boolean = false,
    val heartCount: Int = 0,
)

@Serializable
data class TagVoteResponse(
    val voted: Boolean = false,
    val voteCount: Int = 0,
)

// ── 创作者 / 管理（admin，App 内嵌模块）──
@Serializable
data class AdminBookDto(
    val bookSourceId: String,
    val bookId: String,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val voteCount: Int = 0,
    val ratingCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val hidden: Boolean = false,
)

@Serializable
data class AdminBookListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
    val books: List<AdminBookDto> = emptyList(),
)

@Serializable
data class AdminBookPatchBody(
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val hidden: Boolean? = null,
)

@Serializable
data class AdminBookPatchResponse(
    val ok: Boolean = true,
    val book: AdminBookDto? = null,
)

@Serializable
data class AdminCharacterDto(
    val id: String,
    val bookSourceId: String = "main",
    val bookId: String,
    val name: String,
    val roleType: String = "main", // main / supporting
    val avatarUrl: String? = null,
    val description: String? = null,
    val order: Int = 0,
    val heartCount: Int = 0,
)

@Serializable
data class AdminCharacterListResponse(
    val characters: List<AdminCharacterDto> = emptyList(),
)

@Serializable
data class AdminCharacterUpsertBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val name: String,
    val roleType: String = "main",
    val avatarUrl: String? = null,
    val description: String? = null,
    val order: Int = 0,
)

@Serializable
data class AdminCharacterUpsertResponse(
    val id: String,
    val created: Boolean = false,
    val updated: Boolean = false,
)

@Serializable
data class AdminAnnouncementDto(
    val id: String,
    val title: String,
    val body: String,
    val level: String = "info", // info / warning / important
    val publishedAt: Long = 0,
    val expiresAt: Long? = null,
)

@Serializable
data class AdminAnnouncementListResponse(
    val announcements: List<AdminAnnouncementDto> = emptyList(),
)

@Serializable
data class AdminAnnouncementBody(
    val title: String,
    val body: String,
    val level: String = "info",
)

@Serializable
data class AdminAnnouncementResponse(
    val ok: Boolean = true,
    val announcement: AdminAnnouncementDto? = null,
)

// ── 作者碎碎念 / 作者日志（0.16.5）──
// 三类：musings(碎碎念/脑洞/灵感素材) / announcement(公告·更新计划) / changelog(章节改动说明)。
@Serializable
data class AuthorLogDto(
    val id: String,
    val bookId: String,
    val type: String = "musings", // musings / announcement / changelog
    val title: String,
    val body: String,
    val chapterRef: String? = null, // 关联章节（自由文本，如 "第12章 迷雾"）
    val pinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class AuthorLogListResponse(
    val items: List<AuthorLogDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class AuthorLogCreateBody(
    val bookId: String,
    val type: String = "musings",
    val title: String,
    val body: String,
    val chapterRef: String? = null,
    val pinned: Boolean = false,
)

@Serializable
data class AuthorLogPatchBody(
    val title: String? = null,
    val body: String? = null,
    val type: String? = null,
    val chapterRef: String? = null,
    val pinned: Boolean? = null,
)

@Serializable
data class AuthorLogResponse(
    val ok: Boolean = true,
    val id: String? = null,
    val log: AuthorLogDto? = null,
)

// ══ 书圈经济体（0.17.0）══
@Serializable
data class CoinBalanceDto(
    val userId: String = "",
    val balance: Int = 0,
    val locked: Int = 0, // 锁仓（竞拍押金 / 投资质押）
    val earnedTotal: Int = 0,
)

@Serializable
data class CoinLedgerDto(
    val id: String = "",
    val type: String = "",
    val delta: Int = 0,
    val reason: String? = null,
    val balanceAfter: Int = 0,
    val bookId: String? = null,
    val createdAt: Long = 0,
)

@Serializable
data class CoinLedgerListResponse(
    val items: List<CoinLedgerDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class CircleMembershipDto(
    val userId: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val reputation: Int = 0,
    val role: String = "member",
    val investedShares: Int = 0,
    val claimedAmount: Int = 0, // 已从国库领取的初始书币
)

@Serializable
data class BookCircleDto(
    val bookSourceId: String = "main",
    val bookId: String = "",
    val ownerUserId: String? = null,
    val ownerDisplayName: String? = null,
    val ownerSince: Long? = null,
    val status: String = "open",
    val currentBid: Int = 0, // 当前最高竞拍价（无主时=起拍参考）
    val treasury: Int = 0, // 国库未领取余额（初始铸造 - 已领取）
    val mintedTotal: Int = 0, // 已进入流通的总量
    val claimWindowOpen: Boolean = false, // 初始领取窗口是否开放
    val totalInvestment: Int = 0,
    val growthIndex: Float = 0f,
    val myMembership: CircleMembershipDto? = null,
    val myInvestment: InvestmentDto? = null,
    val lockedBalance: Int = 0, // 我的锁仓总额
    val canBid: Boolean = false, // 当前用户是否可参与竞拍
    val canInvest: Boolean = false,
    val investMaxPerBook: Int = 5000,
    val investLockDays: Int = 14,
)

@Serializable
data class CircleRankItem(
    val userId: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val reputation: Int = 0,
    val role: String = "member",
    val investedShares: Int = 0,
)

@Serializable
data class CircleRankResponse(
    val items: List<CircleRankItem> = emptyList(),
    val totalInvestment: Int = 0,
    val growthIndex: Float = 0f,
    val date: String? = null,
)

@Serializable
data class InvestmentDto(
    val id: String = "",
    val bookId: String = "",
    val amount: Int = 0,
    val sharePct: Float = 0f,
    val investedAt: Long = 0,
    val unlockAt: Long = 0,
    val status: String = "active",
    val returnedTotal: Int = 0,
)

@Serializable
data class InvestmentListResponse(
    val items: List<InvestmentDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class BidResultDto(
    val ok: Boolean = true,
    val ownerUserId: String? = null,
    val ownerSince: Long? = null,
)

@Serializable
data class InvestResultDto(
    val ok: Boolean = true,
    val investmentId: String = "",
    val sharePct: Float = 0f,
)

// 请求体
@Serializable
data class CircleJoinBody(
    val bookId: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class CircleBidBody(
    val bookId: String,
    val bid: Int, // 出价书币（高于当前最高）
)

@Serializable
data class CircleFeatureBody(
    val bookId: String,
    val commentId: String,
)

@Serializable
data class CircleInvestBody(
    val bookId: String,
    val amount: Int,
)

@Serializable
data class CircleBookIdBody(val bookId: String)

@Serializable
data class ClaimResponse(
    val ok: Boolean = true,
    val claimed: Int = 0,
    val treasuryRemaining: Int = 0,
)

@Serializable
data class TransferBody(
    val toUserId: String,
    val amount: Int,
    val reason: String? = null,
    val bookId: String? = null,
)

@Serializable
data class CoinGrantBody(
    val userId: String,
    val amount: Int,
    val reason: String? = null,
)

@Serializable
data class CircleConfigBody(val value: String = "") // Json 字符串透传

@Serializable
data class CircleConfigDto(
    val circleInitialMint: Int = 100000,
    val circleClaimWindowDays: Int = 3,
    val circleClaimPerUser: Int = 100,
    val ownerStartBid: Int = 100,
    val inactiveOwnerDays: Int = 30,
    val investMaxPerBook: Int = 5000,
    val investLockDays: Int = 14,
    val councilTopPct: Int = 10,
    val elderTopPct: Int = 1,
    val ownerMinInvestShares: Int = 100,
)

@Serializable
data class CircleResetOwnerBody(val bookId: String)

// ── P3 广告 / 归因 ──
@Serializable
data class AdResponse(
    val slot: String? = null,
    val creatives: List<AdCreativeDto> = emptyList(),
)

@Serializable
data class AdCreativeDto(
    val id: String,
    val kind: String? = null, // cross_promo / third_party
    val title: String? = null,
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val bookSourceId: String? = null,
    val bookId: String? = null,
)

@Serializable
data class AdImpressionBody(
    val creativeId: String? = null,
    val slot: String,
)

@Serializable
data class AdClickBody(
    val impressionId: String? = null,
)

@Serializable
data class AttributionBody(
    val installChannel: String? = null,
    val referrer: String? = null,
    val campaign: String? = null,
)

// ════════════════════════════════════════════════════════════════
//  P4 书友圈（社区）DTO —— 后端 0.7.2 落地
// ════════════════════════════════════════════════════════════════

@Serializable
data class PostAuthor(
    val id: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null, // 仅详情/评论接口返回；列表接口不含 role
)

@Serializable
data class PostTopic(val id: String, val name: String)

@Serializable
data class PostBooklistRef(val id: String, val title: String? = null, val coverUrl: String? = null)

@Serializable
data class PostItem(
    val id: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val bookSourceId: String? = null,
    val bookId: String? = null,
    val topic: PostTopic? = null,
    val booklist: PostBooklistRef? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = 0,
    val author: PostAuthor,
    val liked: Boolean = false,
)

@Serializable
data class PostListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val posts: List<PostItem> = emptyList(),
    val nextCursor: Long? = null,
)

@Serializable
data class PostCreateBody(
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val bookSourceId: String? = null,
    val bookId: String? = null,
    val topicId: String? = null,
    val booklistId: String? = null,
)

@Serializable
data class PostCreateResponse(val ok: Boolean = true, val id: String = "")

@Serializable
data class PostDetail(
    val id: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val bookSourceId: String? = null,
    val bookId: String? = null,
    val topic: PostTopic? = null,
    val booklist: PostBooklistRef? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = 0,
    val author: PostAuthor,
    val liked: Boolean = false,
    val comments: List<PostCommentItem> = emptyList(),
)

@Serializable
data class PostCommentItem(
    val id: String,
    val parentId: String? = null,
    val content: String,
    val likeCount: Int = 0,
    val createdAt: Long = 0,
    val author: PostAuthor,
)

@Serializable
data class PostCommentListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
    val comments: List<PostCommentItem> = emptyList(),
)

@Serializable
data class PostCommentBody(
    val content: String,
    val parentId: String? = null,
)

@Serializable
data class TopicListResponse(val topics: List<PostTopic> = emptyList())

@Serializable
data class PostUpdateBody(
    val content: String? = null,
    val imageUrls: List<String>? = null,
    val topicId: String? = null, // null 表示清除话题
)

@Serializable
data class LikeResponse(
    val ok: Boolean = true,
    val liked: Boolean = false,
    val likeCount: Int = 0,
)

// ════════════════════════════════════════════════════════════════
//  P4 书单推书（0.7.4）/ 用户主页（0.7.5）/ 激励（0.7.7）/ 治理（0.7.8）/ 运营（0.7.6）DTO
// ════════════════════════════════════════════════════════════════

@Serializable
data class BooklistAuthor(val id: String, val displayName: String? = null, val avatarUrl: String? = null)

@Serializable
data class BooklistItemDto(
    val id: String,
    val bookSourceId: String,
    val bookId: String,
    val bookUid: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val note: String? = null,
    val position: Int = 0,
)

@Serializable
data class BooklistSummary(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val isOfficial: Boolean = false,
    val likeCount: Int = 0,
    val collectCount: Int = 0,
    val itemCount: Int = 0,
    val createdAt: Long = 0,
    val owner: BooklistAuthor,
    val collected: Boolean = false,
)

@Serializable
data class BooklistListResponse(
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val booklists: List<BooklistSummary> = emptyList(),
)

@Serializable
data class BooklistCreateBody(
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val isOfficial: Boolean = false,
)

@Serializable
data class BooklistCreateResponse(val ok: Boolean = true, val id: String = "")

@Serializable
data class BooklistDetail(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val isOfficial: Boolean = false,
    val likeCount: Int = 0,
    val collectCount: Int = 0,
    val collected: Boolean = false,
    val createdAt: Long = 0,
    val owner: BooklistAuthor,
    val items: List<BooklistItemDto> = emptyList(),
)

@Serializable
data class BooklistItemBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val bookUid: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val note: String? = null,
)

@Serializable
data class CollectResponse(
    val ok: Boolean = true,
    val collected: Boolean = false,
    val collectCount: Int = 0,
)

@Serializable
data class BooklistUpdateBody(
    val title: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class BooklistItemUpdateBody(
    val note: String? = null,
    val position: Int? = null,
)

@Serializable
data class UserProfile(
    val id: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null,
    val createdAt: Long = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val booklistCount: Int = 0,
    val isFollowing: Boolean = false,
    val stats: UserStats? = null,
)

@Serializable
data class UserStats(
    val totalMin: Int = 0,
    val days: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
)

@Serializable
data class UserBookshelfItem(
    val bookSourceId: String,
    val bookId: String,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val status: String? = null,
    val updatedAt: Long = 0,
)

@Serializable
data class UserBookshelfResponse(val items: List<UserBookshelfItem> = emptyList())

@Serializable
data class UserSummary(
    val id: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null,
)

@Serializable
data class UserListResponse(val users: List<UserSummary> = emptyList())

@Serializable
data class FollowResponse(
    val ok: Boolean = true,
    val following: Boolean = false,
    val followerCount: Int = 0,
)

@Serializable
data class BlockResponse(val ok: Boolean = true, val blocked: Boolean = false)

@Serializable
data class HotPostsResponse(val posts: List<PostItem> = emptyList())

@Serializable
data class HotBooklistsResponse(val booklists: List<BooklistSummary> = emptyList())

@Serializable
data class ReadingSessionBody(
    val bookSourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val durationSec: Int = 0,
)

@Serializable
data class BadgeDto(
    val key: String,
    val name: String,
    val desc: String? = null,
    val unlocked: Boolean = false,
)

@Serializable
data class ReadingStats(
    val totalMin: Int = 0,
    val days: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val badges: List<BadgeDto> = emptyList(),
)

@Serializable
data class ReportBody(val reason: String? = null)

@Serializable
data class HomeBannerItem(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val title: String? = null,
)

@Serializable
data class HomeAnnouncement(
    val id: String,
    val title: String,
    val body: String? = null,
    val level: String? = null,
    val publishedAt: Long = 0,
)

@Serializable
data class HomeResponse(
    val banner: List<HomeBannerItem>? = null,
    val featuredBooklists: List<BooklistSummary> = emptyList(),
    val announcements: List<HomeAnnouncement> = emptyList(),
)

// ── 0.8.1 自有书库（catalog）DTO ──
@Serializable
data class CatalogBookDto(
    val sourceId: String = BOOK_SOURCE_MAIN,
    val bookId: String,
    val uid: String? = null,
    val title: String? = null,
    val author: String? = null,
    val coverUrl: String? = null, // 仅 http(s)，绝不 data: URI
    val category: String? = null,
    val status: String? = null,
    val wordCount: Int? = null,
    val chapterCount: Int? = null,
    val viewCount: Int? = null,
)

@Serializable
data class CatalogListResponse(
    val books: List<CatalogBookDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1,
)

@Serializable
data class CatalogCategoriesResponse(val categories: List<String> = emptyList())

// ── 0.11.0 标注 / 书签同步响应 DTO ──
@Serializable
data class AnnotationPushResult(
    val clientId: String = "",
    val applied: Boolean = true,
    val reason: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class AnnotationPushAck(val results: List<AnnotationPushResult> = emptyList())

// ── 0.12.0 创意工坊 · 插件广场 DTO ──
// 与 APP 端 data.plugin.PluginManifest 字段对齐；用可空/默认容错未知字段。
@Serializable
data class PluginSummary(
    val pluginId: String,
    val name: String,
    val author: String = "",
    val description: String = "",
    val icon: String = "",
    val type: String = "",
    val minAppVersion: Int = 67,
    val pinned: Boolean = false,
    val installs: Int = 0,
    val likes: Int = 0,
    // 后端自 0.16.4 起在列表/ids 查询中返回审核状态；广场恒为 published，ids 查询用于「我的发布」。
    val status: String = "published",
)

@Serializable
data class PluginListResponse(
    val items: List<PluginSummary> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class PluginManifestResponse(val manifest: PluginManifestDto)

@Serializable
data class PluginManifestDto(
    val id: String,
    val name: String,
    val version: Int = 1,
    val author: String = "",
    val description: String = "",
    val icon: String = "",
    val minAppVersion: Int = 67,
    val type: String,
    val capabilities: PluginCapabilitiesDto = PluginCapabilitiesDto(),
)

@Serializable
data class PluginCapabilitiesDto(
    val annotation: PluginAnnotationCapDto? = null,
    val theme: PluginThemeCapDto? = null,
    val toolbar: PluginToolbarCapDto? = null,
    val decorator: PluginDecoratorCapDto? = null,
)

@Serializable
data class PluginAnnotationCapDto(
    val annotationType: String,
    val label: String,
    val defaultColor: Int? = null,
    val withNote: Boolean = false,
)

@Serializable
data class PluginThemeCapDto(val name: String, val background: Int, val text: Int)

@Serializable
data class PluginToolbarCapDto(
    val action: String,
    val label: String,
    val position: String = "bottom",
)

@Serializable
data class PluginDecoratorCapDto(
    val targetType: String,
    val style: String = "background",
    val color: Int? = null,
)

// ── 0.16.1 创意工坊 · UGC 闭环应答 ──
@Serializable
data class SubmitPluginAck(
    val ok: Boolean = true,
    val status: String = "",
    val pluginId: String = "",
)

@Serializable
data class PluginCounterAck(
    val installs: Int = 0,
    val likes: Int = 0,
)
