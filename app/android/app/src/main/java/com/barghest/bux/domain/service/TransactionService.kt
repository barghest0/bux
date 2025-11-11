package com.barghest.bux.domain.service

import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction

class TransactionService(
    private val repository: TransactionRepository
) {

    suspend fun addTransaction(transaction: NewTransaction) {
        repository.add(transaction)
    }

    suspend fun getTransactions(): List<Transaction> = repository.getAll()
}