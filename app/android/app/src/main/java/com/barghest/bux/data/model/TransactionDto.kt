package com.barghest.bux.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Int,
    val amount: Double
)