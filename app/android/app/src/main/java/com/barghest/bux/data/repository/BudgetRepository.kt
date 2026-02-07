package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateBudgetRequest
import com.barghest.bux.data.local.dao.BudgetDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.entity.BudgetEntity
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.mapper.toBudgetDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Budget
import com.barghest.bux.domain.model.BudgetStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class BudgetRepository(
    private val api: Api,
    private val budgetDao: BudgetDao,
    private val pendingOps: PendingOperationDao,
    private val userIdProvider: () -> Int
) {
    fun getBudgetsFlow(): Flow<List<Budget>> {
        return budgetDao.getByUser(userIdProvider()).map { it.toBudgetDomainList() }
    }

    suspend fun createBudget(
        categoryId: Int,
        amount: BigDecimal,
        currency: String = "RUB",
        period: String = "monthly"
    ): Result<Budget> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = BudgetEntity(
            id = tempId,
            userId = userId,
            categoryId = categoryId,
            categoryName = null,
            amount = amount.toPlainString(),
            currency = currency,
            period = period
        )
        budgetDao.insert(entity)

        val request = CreateBudgetRequest(
            categoryId = categoryId,
            amount = amount.toPlainString(),
            currency = currency,
            period = period
        )
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "budget",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    suspend fun deleteBudget(id: Int): Result<Unit> {
        budgetDao.deleteById(id)

        pendingOps.insert(
            PendingOperationEntity(
                entityType = "budget",
                entityId = id,
                operationType = "delete",
                payload = ""
            )
        )

        return Result.success(Unit)
    }

    // Budget status is computed server-side; cache the budgets list locally
    // and compute status from local transactions when offline
    suspend fun getBudgetStatus(): Result<List<BudgetStatus>> {
        return api.fetchBudgetStatus().map { list -> list.map { it.toDomain() } }
    }

    // Called by SyncManager only
    suspend fun refreshBudgets(): Result<List<Budget>> {
        return api.fetchBudgets().map { budgets ->
            val userId = userIdProvider()
            budgetDao.deleteAllByUser(userId)
            budgetDao.insertAll(budgets.map { it.toEntity(userId) })
            budgets.map { it.toDomain() }
        }
    }
}
