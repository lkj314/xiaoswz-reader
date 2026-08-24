package com.xiaoswz.reader.data.api

import retrofit2.http.GET
import retrofit2.http.Query

// 书币账本（0.17.0）：余额 + 透明流水
interface CoinApi {
    @GET("api/coin/balance")
    suspend fun getCoinBalance(): CoinBalanceDto

    @GET("api/coin/ledger")
    suspend fun getCoinLedger(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
    ): CoinLedgerListResponse
}
