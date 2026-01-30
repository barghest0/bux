package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateBudgetRequest
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Budget
import com.barghest.bux.domain.model.BudgetStatus
import java.math.BigDecimal

class BudgetRepository(private val api: Api) {

    suspend fun getBudgets(): Result<List<Budget>> {
        return api.fetchBudgets().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getBudgetStatus(): Result<List<BudgetStatus>> {
        return api.fetchBudgetStatus().map { list -> list.map { it.toDomain() } }
    }

    suspend fun createBudget(
        categoryId: Int,
        amount: BigDecimal,
        currency: String = "RUB",
        period: String = "monthly"
    ): Result<Budget> {
        val request = CreateBudgetRequest(
            categoryId = categoryId,
            amount = amount.toPlainString(),
            currency = currency,
            period = period
        )
        return api.createBudget(request).map { it.toDomain() }
    }

    suspend fun deleteBudget(id: Int): Result<Unit> {
        return api.deleteBudget(id)
    }
}
