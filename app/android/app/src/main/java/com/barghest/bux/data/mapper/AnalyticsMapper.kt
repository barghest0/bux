package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.CategorySummaryResponse
import com.barghest.bux.data.dto.MonthlySummaryResponse
import com.barghest.bux.data.dto.TransactionSummaryResponse
import com.barghest.bux.domain.model.CategorySummary
import com.barghest.bux.domain.model.MonthlySummary
import com.barghest.bux.domain.model.TransactionSummary
import java.math.BigDecimal

fun CategorySummaryResponse.toDomain(): CategorySummary = CategorySummary(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    type = type,
    total = total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    count = count
)

fun MonthlySummaryResponse.toDomain(): MonthlySummary = MonthlySummary(
    year = year,
    month = month,
    income = income.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    expense = expense.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    net = net.toBigDecimalOrNull() ?: BigDecimal.ZERO
)

fun TransactionSummaryResponse.toDomain(): TransactionSummary = TransactionSummary(
    totalIncome = totalIncome.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    totalExpense = totalExpense.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    net = net.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    byCategory = byCategory.map { it.toDomain() },
    byMonth = byMonth.map { it.toDomain() }
)
