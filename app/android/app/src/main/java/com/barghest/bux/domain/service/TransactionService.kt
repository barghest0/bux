package com.barghest.bux.domain.service

import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction

class TransactionService(
    private val repository: TransactionRepository
) {
    suspend fun addTransaction(transaction: NewTransaction): Result<Unit> {
        return repository.add(transaction)
    }

    suspend fun getTransactions(): Result<List<Transaction>> {
        return repository.getAll()
    }
}
