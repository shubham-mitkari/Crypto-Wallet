package com.example.crypwallet

import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoApi {
    @GET("simple/price")
    suspend fun getPrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") vs: String = "inr"
    ): Map<String, Map<String, Double>>

    @GET("coins/markets")
    suspend fun getMarketCoins(
        @Query("vs_currency") vsCurrency: String,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false
    ): List<CoinMarket>


    data class CoinMarket(
        val id: String,
        val current_price: Double
    )
}