package com.barghest.bux.data.repository

import com.barghest.bux.data.local.dao.RecurringTransactionDao
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.mapper.toRecurringEntityDomainList
import com.barghest.bux.data.mapper.toRecurringDomainList
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewRecurringTransaction
import com.barghest.bux.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringTransactionRepository(
    private val api: Api,
    private val dao: RecurringTransactionDao,
    private val userIdProvider: () -> Int
) {
    fun getFlow(): Flow<List<RecurringTransaction>> {
        return dao.getByUser(userIdProvider()).map { it.toRecurringEntityDomainList() }
    }

    suspend fun refresh(): Result<List<RecurringTransaction>> {
        return api.fetchRecurringTransactions().map { list ->
            val userId = userIdProvider()
            dao.deleteAllByUser(userId)
            dao.insertAll(list.map { it.toEntity(userId) })
            list.toRecurringDomainList()
        }
    }

    suspend fun create(rt: NewRecurringTransaction): Result<RecurringTransaction> {
        return api.createRecurringTransaction(rt.toRequest()).map { response ->
            dao.insert(response.toEntity(userIdProvider()))
            response.toDomain()
        }
    }

    suspend fun toggleActive(id: Int): Result<RecurringTransaction> {
        return api.toggleRecurringTransaction(id).map { response ->
            dao.insert(response.toEntity(userIdProvider()))
            response.toDomain()
        }
    }

    suspend fun execute(id: Int): Result<RecurringTransaction> {
        return api.executeRecurringTransaction(id).map { response ->
            dao.insert(response.toEntity(userIdProvider()))
            response.toDomain()
        }
    }

    suspend fun delete(id: Int): Result<Unit> {
        return api.deleteRecurringTransaction(id).also {
            if (it.isSuccess) dao.deleteById(id)
        }
    }
}
