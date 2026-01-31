package com.barghest.bux.data.repository

import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toDomainList
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.mapper.toTransactionDomainList
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val api: Api,
    private val transactionDao: TransactionDao,
    private val userIdProvider: () -> Int
) {
    fun getTransactionsFlow(): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByUser(userIdProvider())
            .map { it.toTransactionDomainList() }
    }

    fun getTransactionsByAccountFlow(accountId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId)
            .map { it.toTransactionDomainList() }
    }

    suspend fun refreshTransactions(): Result<List<Transaction>> {
        return api.fetchTransactions().map { transactions ->
            val userId = userIdProvider()
            val entities = transactions.map { it.toEntity(userId) }
            transactionDao.insertAll(entities)
            transactions.toDomainList()
        }
    }

    suspend fun refreshTransactionsByAccount(accountId: Int): Result<List<Transaction>> {
        return api.fetchTransactionsByAccount(accountId).map { transactions ->
            val userId = userIdProvider()
            val entities = transactions.map { it.toEntity(userId) }
            transactionDao.insertAll(entities)
            transactions.toDomainList()
        }
    }

    suspend fun exportCSV(): Result<ByteArray> {
        return api.exportTransactionsCSV()
    }

    suspend fun createTransaction(transaction: NewTransaction): Result<Transaction> {
        return api.createTransaction(transaction.toRequest()).map { response ->
            val userId = userIdProvider()
            transactionDao.insert(response.toEntity(userId))
            response.toDomain()
        }
    }
}
