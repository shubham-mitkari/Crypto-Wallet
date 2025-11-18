package com.example.crypwallet

data class TransactionItem(
    val type: TxType,
    val addressOrHash: String,
    val timestamp: Long,
    val amount: String,
    val counterparty: String
)

enum class TxType { SENT, RECEIVED }