package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendItemResponse(
    val year: Int,
    val month: Int,
    val income: String,
    val expense: String,
    val net: String,
    @SerialName("prev_income") val prevIncome: String? = null,
    @SerialName("prev_expense") val prevExpense: String? = null
)

@Serializable
data class TrendsResponse(
    val trends: List<TrendItemResponse>
)

@Serializable
data class TopCategoryItem(
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_icon") val categoryIcon: String = "",
    @SerialName("category_color") val categoryColor: String = "",
    val type: String,
    val total: String,
    val count: Long
)

@Serializable
data class TopCategoriesResponse(
    val categories: List<TopCategoryItem>
)
