package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetResponse(
    val id: Int,
    @SerialName("category_id") val categoryId: Int,
    val category: CategoryResponse? = null,
    val amount: String,
    val currency: String,
    val period: String
)

@Serializable
data class CreateBudgetRequest(
    @SerialName("category_id") val categoryId: Int,
    val amount: String,
    val currency: String = "RUB",
    val period: String = "monthly"
)

@Serializable
data class UpdateBudgetRequest(
    val amount: String? = null,
    val period: String? = null
)

@Serializable
data class BudgetStatusResponse(
    @SerialName("budget_id") val budgetId: Int,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_icon") val categoryIcon: String,
    @SerialName("category_color") val categoryColor: String,
    @SerialName("budget_amount") val budgetAmount: String,
    @SerialName("spent_amount") val spentAmount: String,
    val remaining: String,
    @SerialName("spent_percent") val spentPercent: String,
    val period: String,
    val currency: String
)
