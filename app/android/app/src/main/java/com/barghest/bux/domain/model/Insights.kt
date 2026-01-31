package com.barghest.bux.domain.model

import java.math.BigDecimal

data class TrendItem(
    val year: Int,
    val month: Int,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal,
    val prevIncome: BigDecimal?,
    val prevExpense: BigDecimal?
)

data class TopCategory(
    val categoryId: Int?,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val total: BigDecimal,
    val count: Long
)
