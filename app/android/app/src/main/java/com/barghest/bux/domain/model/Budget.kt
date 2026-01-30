package com.barghest.bux.domain.model

import java.math.BigDecimal

data class Budget(
    val id: Int,
    val categoryId: Int,
    val categoryName: String?,
    val amount: BigDecimal,
    val currency: String,
    val period: BudgetPeriod
)

data class BudgetStatus(
    val budgetId: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val budgetAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val remaining: BigDecimal,
    val spentPercent: BigDecimal,
    val period: BudgetPeriod,
    val currency: String
)

enum class BudgetPeriod(val value: String) {
    MONTHLY("monthly"),
    YEARLY("yearly");

    companion object {
        fun fromValue(value: String): BudgetPeriod {
            return entries.find { it.value == value } ?: MONTHLY
        }
    }
}
