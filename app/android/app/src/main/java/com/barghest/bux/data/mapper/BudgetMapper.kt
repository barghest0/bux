package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.BudgetResponse
import com.barghest.bux.data.dto.BudgetStatusResponse
import com.barghest.bux.data.local.entity.BudgetEntity
import com.barghest.bux.domain.model.Budget
import com.barghest.bux.domain.model.BudgetPeriod
import com.barghest.bux.domain.model.BudgetStatus
import java.math.BigDecimal

fun BudgetResponse.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = category?.name,
    amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    currency = currency,
    period = BudgetPeriod.fromValue(period)
)

fun BudgetResponse.toEntity(userId: Int): BudgetEntity = BudgetEntity(
    id = id,
    userId = userId,
    categoryId = categoryId,
    categoryName = category?.name,
    amount = amount,
    currency = currency,
    period = period
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    currency = currency,
    period = BudgetPeriod.fromValue(period)
)

fun List<BudgetEntity>.toBudgetDomainList(): List<Budget> = map { it.toDomain() }

fun BudgetStatusResponse.toDomain(): BudgetStatus = BudgetStatus(
    budgetId = budgetId,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    budgetAmount = budgetAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    spentAmount = spentAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    remaining = remaining.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    spentPercent = spentPercent.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    period = BudgetPeriod.fromValue(period),
    currency = currency
)
