package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategorySummaryResponse(
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_icon") val categoryIcon: String,
    @SerialName("category_color") val categoryColor: String,
    val type: String,
    val total: String,
    val count: Int
)

@Serializable
data class MonthlySummaryResponse(
    val year: Int,
    val month: Int,
    val income: String,
    val expense: String,
    val net: String
)

@Serializable
data class TransactionSummaryResponse(
    @SerialName("total_income") val totalIncome: String,
    @SerialName("total_expense") val totalExpense: String,
    val net: String,
    @SerialName("by_category") val byCategory: List<CategorySummaryResponse>,
    @SerialName("by_month") val byMonth: List<MonthlySummaryResponse>
)
