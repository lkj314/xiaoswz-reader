package com.xiaoswz.reader.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 书圈经济体（0.17.0 → 0.18 书圈金融模拟器）：书圈主页 / 加入 / 精选 / 投资 / 持股治理 / 交易所 / 董事会 / 新闻
interface BookCircleApi {
    @GET("api/book-circle")
    suspend fun getBookCircle(
        @Query("bookId") bookId: String,
    ): BookCircleDto

    @POST("api/book-circle/join")
    suspend fun joinBookCircle(@Body body: CircleJoinBody): BookCircleDto

    @POST("api/book-circle/feature")
    suspend fun featureComment(@Body body: CircleFeatureBody): OkAck

    @POST("api/book-circle/invest")
    suspend fun investBook(@Body body: CircleInvestBody): InvestResultDto

    @POST("api/book-circle/claim")
    suspend fun claimInitial(@Body body: CircleBookIdBody): ClaimResponse

    @POST("api/book-circle/transfer")
    suspend fun transferCoins(@Body body: TransferBody): OkAck

    @POST("api/book-circle/withdraw")
    suspend fun withdrawInvestment(@Body body: CircleBookIdBody): OkAck

    @GET("api/book-circle/investment")
    suspend fun getInvestment(
        @Query("bookId") bookId: String,
        @Query("page") page: Int = 1,
    ): InvestmentListResponse

    @GET("api/book-circle/rank")
    suspend fun getCircleRank(
        @Query("bookId") bookId: String,
        @Query("date") date: String? = null,
    ): CircleRankResponse

    // ── 0.18 书圈专区（导航【书圈】Tab 聚合）──
    @GET("api/book-circle/hub")
    suspend fun getCircleHub(): CircleHubResponse

    // ── 0.18 书币交易所 ──
    @POST("api/exchange/order")
    suspend fun placeExchangeOrder(@Body body: ExchangePlaceBody): OkAck

    @POST("api/exchange/fill")
    suspend fun fillExchangeOrder(@Body body: ExchangeFillBody): OkAck

    @POST("api/exchange/cancel")
    suspend fun cancelExchangeOrder(@Body body: ExchangeCancelBody): OkAck

    @GET("api/exchange/book")
    suspend fun getOrderBook(
        @Query("fromBookId") fromBookId: String,
        @Query("toBookId") toBookId: String,
    ): OrderBookResponse

    @GET("api/exchange/price")
    suspend fun getPriceHistory(
        @Query("bookId") bookId: String,
        @Query("limit") limit: Int = 60,
    ): PriceHistoryResponse

    // ── 0.18 董事会（董事权限）──
    @POST("api/book-circle/board/anchor")
    suspend fun boardSetAnchor(@Body body: BoardAnchorBody): AnchorPriceResponse

    @POST("api/book-circle/board/buyback")
    suspend fun boardBuyback(@Body body: BoardBuybackBody): BuybackResponse

    @POST("api/book-circle/board/reserve")
    suspend fun boardMoveReserve(@Body body: BoardReserveBody): OkAck

    // ── 0.18 书圈新闻稿 / 财报 ──
    @GET("api/book-circle/news")
    suspend fun getCircleNews(
        @Query("bookId") bookId: String,
    ): NewsListResponse

    @POST("api/book-circle/news")
    suspend fun publishCircleNews(@Body body: NewsPublishBody): NewsPublishResponse

    // ── 0.19 个人钱包 + 理财产品 + 稳定币 ──
    @GET("api/circle-fund")
    suspend fun getFundDiscovery(): FundDiscoveryResponse

    @POST("api/circle-fund")
    suspend fun createFund(@Body body: CreateFundBody): CreateFundResponse

    @GET("api/circle-fund/{fundId}")
    suspend fun getFundDetail(
        @retrofit2.http.Path("fundId") fundId: String,
    ): FundDetailResponse

    @POST("api/circle-fund/subscribe")
    suspend fun subscribeFund(@Body body: SubscribeBody): SubscribeResponse

    @POST("api/circle-fund/redeem")
    suspend fun redeemFund(@Body body: RedeemBody): RedeemResponse

    @POST("api/circle-fund/grab")
    suspend fun grabStableCoin(@Body body: GrabBody): GrabResponse

    @POST("api/circle-fund/transfer-stablecoin")
    suspend fun transferStableCoin(@Body body: TransferStableBody): TransferStableResponse

    @GET("api/circle-fund/stablecoin")
    suspend fun getStablecoinStatus(): StablecoinStatusResponse
}
