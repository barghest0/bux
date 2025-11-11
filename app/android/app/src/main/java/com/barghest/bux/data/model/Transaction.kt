package com.barghest.bux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val id: Int,
    val amount: Double
)

@Serializable
data class TransactionRequest(
    val amount: Double,
    val currency: String
)