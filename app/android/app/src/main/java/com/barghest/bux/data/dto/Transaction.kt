package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(
    val id: Int,
    @SerialName("account_id") val accountId: Int,
    @SerialName("destination_account_id") val destinationAccountId: Int? = null,
    val type: String,
    val status: String,
    val amount: String,
    val currency: String,
    val description: String? = null,
    @SerialName("transaction_date") val transactionDate: String
)

@Serializable
data class CreateTransactionRequest(
    @SerialName("account_id") val accountId: Int,
    @SerialName("destination_account_id") val destinationAccountId: Int? = null,
    val type: String,
    val amount: String,
    val currency: String,
    @SerialName("category_id") val categoryId: Int? = null,
    val description: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null
)
