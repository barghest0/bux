package com.barghest.bux.data.repository

import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.model.TransactionDto
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Transaction

class TransactionRepository(
    private val api: Api
) {
    suspend fun getAll(): List<Transaction> {
        return api.fetchTransactions().map { it.toDomain() }
    }

    suspend fun add(transaction: Transaction) {
        api.postTransaction(
            TransactionDto(
                id = transaction.id,
                amount = transaction.amount
            )
        )
    }
}