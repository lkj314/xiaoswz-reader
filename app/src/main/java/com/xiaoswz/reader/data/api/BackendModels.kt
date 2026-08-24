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

// ══ 书圈经济体（0.17.0 → 0.18 书圈金融模拟器）══
@Serializable
data class CoinBalanceDto(
    val userId: String = "",
    val balance: Int = 0,
    val locked: Int = 0, // 聚合锁仓（所有书币锁仓之和）
    val earnedTotal: Int = 0,
    val coins: List<BookCoinBalance> = emptyList(), // 0.18 多书币：按书拆分持仓
)

/** 单书币持仓（0.18 多书币） */
@Serializable
data class BookCoinBalance(
    val bookId: String = "",
    val balance: Int = 0,
    val locked: Int = 0, // 锁仓（投资质押 / 交易所挂单）
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
    val treasury: Int = 0, // 国库未领取余额（初始铸造 - 已领取）
    val mintedTotal: Int = 0, // 已进入流通的总量
    val policyAnchorPrice: Float = 1f, // 0.18 董事会锚定的基准书币价（信号）
    val claimWindowOpen: Boolean = false, // 初始领取窗口是否开放（国库>0）
    val totalInvestment: Int = 0,
    val growthIndex: Float = 0f,
    val circulatingSupply: Int = 0, // 流通量 = 投资锁仓 + 已领取
    val shareTotal: Int = 0, // 股权总份额（= 全书活跃投资总额）
    val shareholders: List<ShareholderDto> = emptyList(), // 股东名册 top10
    val reserves: List<ReserveDto> = emptyList(), // 书圈持有的他书币储备
    val myMembership: CircleMembershipDto? = null,
    val myInvestment: InvestmentDto? = null,
    val myCoin: MyBookCoinDto = MyBookCoinDto(), // 我的本书币持仓
    val canInvest: Boolean = false,
    val investMaxPerBook: Int = 5000,
    val investLockDays: Int = 14,
)

/** 股东条目（0.18 股权结构） */
@Serializable
data class ShareholderDto(
    val userId: String = "",
    val displayName: String? = null,
    val role: String = "member",
    val shares: Int = 0, // 投资份额（= 锁仓书币数）
    val pct: Float = 0f, // 占全书总份额比例 %
)

/** 书圈储备条目（0.18）：本书圈持有某他书币的数量（可为负，表示自我储备抵消） */
@Serializable
data class ReserveDto(
    val assetBookId: String = "",
    val amount: Int = 0,
)

/** 单书币持仓视图（0.18） */
@Serializable
data class MyBookCoinDto(
    val balance: Int = 0,
    val locked: Int = 0,
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
    val bookId: String, // 0.18 多书币：转账必须指定书币所属的书
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
    val circleClaimWindowDays: Int = 3, // 已弃用（0.18 领取窗口由 treasury>0 推导）
    val circleClaimPerUser: Int = 100,
    val ownerStartBid: Int = 100, // 已弃用（0.18 圈主改由持股决定，无竞拍）
    val inactiveOwnerDays: Int = 30, // 已弃用
    val investMaxPerBook: Int = 5000,
    val investLockDays: Int = 14,
    val councilTopPct: Int = 10,
    val elderTopPct: Int = 1,
    val ownerMinInvestShares: Int = 100,
    // ── 0.18 书圈金融模拟器新增 ──
    val anchorBookUid: String = "B000001", // 基准书（第一本书）币，作为汇率锚
    val exchangeFeePct: Int = 0, // 交易所费率（系统零抽成）
    val chairmanMarginPct: Int = 10, // 圈主稳定边际 %
    val chairmanCooldownHrs: Int = 24, // 圈主重算冷却小时
    val boardQuorumPct: Int = 30, // 董事会决议法定占比 %
)

@Serializable
data class CircleResetOwnerBody(val bookId: String)

// ── 0.18 书圈金融模拟器：导航【书圈】专区聚合（0.19 升级为个人钱包）──
@Serializable
data class CircleHubResponse(
    val totalNetWorth: Double = 0.0, // 资产净值（Σ(余额+锁仓)×锚定价）
    val joinedCircles: Int = 0, // 已加入/持币书圈数
    val investedBooks: Int = 0, // 已投资书籍数
    val books: List<HubBookDto> = emptyList(),
    // 0.19 个人钱包：持有的理财产品聚合
    val funds: List<HubFundDto> = emptyList(),
    val totalFundValue: Double = 0.0, // 理财产品总市值（以锚定币计）
    val totalFundCost: Double = 0.0, // 理财产品总投入成本
    val totalFundYieldPct: Double = 0.0, // 理财产品总收益率 %
)

@Serializable
data class HubBookDto(
    val bookId: String = "",
    val anchorPrice: Float = 1f, // 该书币基准价
    val treasury: Int = 0, // 国库剩余
    val myBalance: Int = 0, // 我的该书币余额
    val myLocked: Int = 0, // 我的该书币锁仓
    val myInvested: Int = 0, // 我的投资份额
    val myShares: Int = 0, // 投资份额（同 myInvested）
    val mySharePct: Float = 0f, // 我的持股占比 %
    val isChairman: Boolean = false, // 是否本书圈主（董事长）
    val isDirector: Boolean = false, // 是否董事（含圈主/长老/议事员）
    val role: String = "member",
    val netValue: Double = 0.0, // 该书币资产净值
)

// 0.19 个人钱包：持有的理财产品概要
@Serializable
data class HubFundDto(
    val fundId: String = "",
    val bookId: String = "",
    val name: String = "",
    val navPerShare: Double = 1.0,
    val status: String = "active",
    val shares: Double = 0.0, // 持有份额
    val value: Double = 0.0, // 当前市值
    val costBasis: Double = 0.0, // 投入成本
    val yieldPct: Double = 0.0, // 收益率 %
)

// ── 0.18 书币交易所 ──
@Serializable
data class ExchangePlaceBody(
    val fromBookId: String,
    val toBookId: String,
    val fromAmount: Int, // 挂出 fromBook 数量
    val toAmount: Int, // 期望换入 toBook 数量
)

@Serializable
data class ExchangeFillBody(val orderId: String)

@Serializable
data class ExchangeCancelBody(val orderId: String)

@Serializable
data class OrderBookResponse(
    val orders: List<ExchangeOrderDto> = emptyList(),
)

@Serializable
data class ExchangeOrderDto(
    val orderId: String = "",
    val makerId: String = "",
    val fromAmount: Int = 0,
    val toAmount: Int = 0,
    val rate: Float = 0f, // 每 1 fromBook 可换 toBook 数
    val createdAt: Long = 0,
)

@Serializable
data class PriceHistoryResponse(
    val points: List<PricePointDto> = emptyList(),
)

@Serializable
data class PricePointDto(
    val price: Float = 0f,
    val anchorPrice: Float = 0f,
    val volume: Int = 0,
    val ts: Long = 0,
)

// ── 0.18 董事会（董事权限操作）──
@Serializable
data class BoardAnchorBody(
    val bookId: String,
    val price: Float, // 设定基准书币价（信号）
)

@Serializable
data class BoardBuybackBody(
    val bookId: String,
    val amount: Int, // 用本书 treasury 回购锁入储备
)

@Serializable
data class BoardReserveBody(
    val circleBookId: String, // 操作方书圈
    val assetBookId: String, // 换入的他书币
    val amount: Int,
)

@Serializable
data class AnchorPriceResponse(
    val ok: Boolean = true,
    val anchorPrice: Float = 1f,
)

@Serializable
data class BuybackResponse(
    val ok: Boolean = true,
    val treasuryRemaining: Int = 0,
)

// ── 0.18 书圈新闻稿 / 财报 ──
@Serializable
data class NewsPublishBody(
    val bookId: String,
    val type: String = "report", // report / event / announcement
    val title: String,
    val body: String,
    val sentiment: String = "neutral", // bull / bear / neutral
    val statsJson: String? = null, // 运营数据（JSON 字符串透传）
    val roadmapJson: String? = null, // 作者更新蓝图（JSON 字符串透传）
)

@Serializable
data class NewsListResponse(
    val news: List<NewsItemDto> = emptyList(),
)

@Serializable
data class NewsItemDto(
    val id: String = "",
    val type: String = "report",
    val title: String = "",
    val body: String = "",
    val sentiment: String = "neutral",
    val statsJson: String? = null,
    val roadmapJson: String? = null,
    val authorId: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class NewsPublishResponse(
    val ok: Boolean = true,
    val newsId: String = "",
)

// ── 0.19 个人钱包 + 理财产品(CircleFund) + 稳定币(StableCoin) ──
@Serializable
data class FundDiscoveryResponse(
    val funds: List<FundSummaryDto> = emptyList(),
)

@Serializable
data class FundSummaryDto(
    val fundId: String = "",
    val bookId: String = "",
    val name: String = "",
    val description: String? = null,
    val navPerShare: Double = 1.0,
    val totalShares: Double = 0.0,
    val status: String = "active",
    val lastDayReturn: Double = 0.0,
    val cumulativeReturnPct: Double = 0.0,
    val stableCount: Int = 0,
    val createdAt: Long = 0,
    val myShares: Double = 0.0,
    val myValue: Double = 0.0,
    val myCostBasis: Double = 0.0,
    val myYieldPct: Double = 0.0,
)

@Serializable
data class FundDetailResponse(
    val fundId: String = "",
    val bookId: String = "",
    val name: String = "",
    val description: String? = null,
    val createdBy: String = "",
    val status: String = "active",
    val navPerShare: Double = 1.0,
    val totalShares: Double = 0.0,
    val lastDayReturn: Double = 0.0,
    val cumulativeReturnPct: Double = 0.0,
    val consecutiveNegDays: Int = 0,
    val assets: List<FundAssetDto> = emptyList(),
    val stablecoins: List<StableCoinDto> = emptyList(),
    val history: List<FundYieldPointDto> = emptyList(),
    val stableReserve: StableReserveDto? = null,
    val myShares: Double = 0.0,
    val myValue: Double = 0.0,
    val myCostBasis: Double = 0.0,
    val myYieldPct: Double = 0.0,
)

@Serializable
data class FundAssetDto(
    val assetType: String = "book_coin", // book_coin / stablecoin
    val assetBookId: String = "",
    val weightPct: Double = 0.0,
    val balance: Int = 0,
    val value: Double = 0.0,
)

@Serializable
data class StableCoinDto(
    val serial: Int = 0,
    val ownerFundId: String = "",
    val status: String = "",
    val producedAt: Long = 0,
)

@Serializable
data class FundYieldPointDto(
    val date: String = "",
    val navPerShare: Double = 1.0,
    val dayReturnPct: Double = 0.0,
    val totalAssets: Int = 0,
    val dropAttempted: Boolean = false,
    val droppedSerial: Int? = null,
)

@Serializable
data class StableReserveDto(
    val issuedCount: Int = 0,
    val hardCap: Int = 0,
    val lockedB000001: Int = 0,
    val backing: Int = 0,
    val currentProbability: Double = 0.0,
)

@Serializable
data class CreateFundBody(
    val bookId: String,
    val name: String,
    val description: String? = null,
    val assets: List<FundAssetInput> = emptyList(),
)

@Serializable
data class FundAssetInput(
    val assetBookId: String,
    val weightPct: Double = 0.0,
)

@Serializable
data class SubscribeBody(
    val fundId: String,
    val payBookId: String,
    val payAmount: Int,
)

@Serializable
data class RedeemBody(
    val fundId: String,
    val shares: Double,
)

@Serializable
data class GrabBody(val fundId: String)

@Serializable
data class TransferStableBody(
    val serial: Int,
    val toFundId: String,
)

@Serializable
data class CreateFundResponse(
    val ok: Boolean = true,
    val fundId: String = "",
)

@Serializable
data class SubscribeResponse(
    val ok: Boolean = true,
    val shares: Double = 0.0,
    val nav: Double = 1.0,
    val valuePaid: Double = 0.0,
    val myShares: Double = 0.0,
    val myValue: Double = 0.0,
)

@Serializable
data class RedeemResponse(
    val ok: Boolean = true,
    val value: Double = 0.0,
)

@Serializable
data class GrabResponse(
    val dropped: Boolean = false,
    val reason: String? = null,
    val serial: Int? = null,
    val backing: Int? = null,
    val probability: Double? = null,
)

@Serializable
data class TransferStableResponse(
    val ok: Boolean = true,
)

@Serializable
data class StablecoinStatusResponse(
    val issuedCount: Int = 0,
    val hardCap: Int = 0,
    val lockedB000001: Int = 0,
    val backing: Int = 0,
    val currentProbability: Double = 0.0,
)

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
