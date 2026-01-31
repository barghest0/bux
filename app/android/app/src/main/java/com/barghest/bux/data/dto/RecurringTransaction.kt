package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecurringTransactionResponse(
    val id: Int,
    @SerialName("account_id") val accountId: Int,
    val type: String,
    val amount: String,
    val currency: String,
    val description: String? = null,
    val frequency: String,
    @SerialName("next_date") val nextDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    val category: CategoryResponse? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateRecurringTransactionRequest(
    @SerialName("account_id") val accountId: Int,
    val type: String,
    val amount: String,
    val currency: String,
    val description: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val frequency: String,
    @SerialName("next_date") val nextDate: String,
    @SerialName("end_date") val endDate: String? = null
)
