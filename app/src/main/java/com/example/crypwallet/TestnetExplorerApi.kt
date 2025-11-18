package com.example.crypwallet

import retrofit2.http.GET
import retrofit2.http.Path

interface TestnetExplorerApi {
    @GET("address/{address}/txs")
    suspend fun getTransactions(
        @Path("address") address: String
    ): List<TransactionResponse>

    @GET("address/{address}")
    suspend fun getAddressInfo(
        @Path("address") address: String
    ): AddressResponse
}

data class TransactionResponse(
    val txid: String,
    val status: Status,
    val vin: List<Vin>,
    val vout: List<Vout>,
    val received: String
)

data class Status(
    val confirmed: Boolean,
    val block_height: Int?,
    val block_time: Int?
)

data class Vin(
    val prevout: Prevout?
)

data class Prevout(
    val scriptpubkey_address: String?,
    val value: Long?
)

data class Vout(
    val scriptpubkey_address: String?,
    val value: Long?
)

data class AddressResponse(
    val address: String,
    val chain_stats: ChainStats,
    val mempool_stats: ChainStats
)

data class ChainStats(
    val funded_txo_sum: Long,
    val spent_txo_sum: Long,
    val tx_count: Int
)