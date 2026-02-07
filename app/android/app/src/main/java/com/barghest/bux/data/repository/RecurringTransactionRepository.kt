package com.barghest.bux.data.repository

import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.dao.RecurringTransactionDao
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.local.entity.RecurringTransactionEntity
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.mapper.toRecurringDomainList
import com.barghest.bux.data.mapper.toRecurringEntityDomainList
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewRecurringTransaction
import com.barghest.bux.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RecurringTransactionRepository(
    private val api: Api,
    private val dao: RecurringTransactionDao,
    private val pendingOps: PendingOperationDao,
    private val userIdProvider: () -> Int
) {
    fun getFlow(): Flow<List<RecurringTransaction>> {
        return dao.getByUser(userIdProvider()).map { it.toRecurringEntityDomainList() }
    }

    suspend fun create(rt: NewRecurringTransaction): Result<RecurringTransaction> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = RecurringTransactionEntity(
            id = tempId,
            userId = userId,
            accountId = rt.accountId,
            type = rt.type.value,
            amount = rt.amount.toPlainString(),
            currency = rt.currency,
            categoryId = rt.categoryId,
            description = rt.description,
            frequency = rt.frequency.value,
            nextDate = rt.nextDate.toEpochMilli(),
            endDate = rt.endDate?.toEpochMilli(),
            isActive = true
        )
        dao.insert(entity)

        val request = rt.toRequest()
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "recurring_transaction",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    suspend fun toggleActive(id: Int): Result<RecurringTransaction> {
        val existing = dao.getById(id)
        if (existing != null) {
            val toggled = existing.copy(isActive = !existing.isActive)
            dao.insert(toggled)

            pendingOps.insert(
                PendingOperationEntity(
                    entityType = "recurring_transaction",
                    entityId = id,
                    operationType = "toggle",
                    payload = ""
                )
            )

            return Result.success(toggled.toDomain())
        }
        return Result.failure(Exception("Not found"))
    }

    suspend fun execute(id: Int): Result<RecurringTransaction> {
        // Execute needs server — enqueue and return current state
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "recurring_transaction",
                entityId = id,
                operationType = "execute",
                payload = ""
            )
        )
        val existing = dao.getById(id)
            ?: return Result.failure(Exception("Not found"))
        return Result.success(existing.toDomain())
    }

    suspend fun delete(id: Int): Result<Unit> {
        dao.deleteById(id)

        pendingOps.insert(
            PendingOperationEntity(
                entityType = "recurring_transaction",
                entityId = id,
                operationType = "delete",
                payload = ""
            )
        )

        return Result.success(Unit)
    }

    // Called by SyncManager only
    suspend fun refresh(): Result<List<RecurringTransaction>> {
        return api.fetchRecurringTransactions().map { list ->
            val userId = userIdProvider()
            dao.deleteAllByUser(userId)
            dao.insertAll(list.map { it.toEntity(userId) })
            list.toRecurringDomainList()
        }
    }
}
