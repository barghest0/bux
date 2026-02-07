package com.barghest.bux.domain.service

import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionService(
    private val repository: TransactionRepository
) {
    fun getTransactionsFlow(): Flow<List<Transaction>> {
        return repository.getTransactionsFlow()
    }

    fun getTransactionsByAccountFlow(accountId: Int): Flow<List<Transaction>> {
        return repository.getTransactionsByAccountFlow(accountId)
    }

    suspend fun refreshTransactions(): Result<List<Transaction>> {
        return repository.refreshTransactions()
    }

    suspend fun refreshTransactionsByAccount(accountId: Int): Result<List<Transaction>> {
        return repository.refreshTransactionsByAccount(accountId)
    }

    suspend fun createTransaction(transaction: NewTransaction): Result<Transaction> {
        return repository.createTransaction(transaction)
    }

    suspend fun exportCSV(): Result<ByteArray> {
        return repository.exportCSV()
    }
}
