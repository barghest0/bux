package com.barghest.bux.domain.service

import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.model.Transaction

class TransactionService(
    private val repository: TransactionRepository
) {
    suspend fun getBalance(): Double {
        val list = repository.getAll()
        return 0.0
    }

    suspend fun addTransaction(transaction: Transaction) {
        repository.add(transaction)
    }

    suspend fun getTransactions(): List<Transaction> = repository.getAll()
}