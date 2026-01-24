package com.barghest.bux.data.repository

import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction

class TransactionRepository(
    private val api: Api
) {
    suspend fun getAll(): Result<List<Transaction>> {
        return api.fetchTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun add(transaction: NewTransaction): Result<Unit> {
        return api.postTransaction(transaction.toRequest())
    }
}
