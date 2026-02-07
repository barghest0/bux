package com.barghest.bux.data.repository

import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.local.entity.TransactionEntity
import com.barghest.bux.data.mapper.toDomainList
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.mapper.toTransactionDomainList
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionStatus
import com.barghest.bux.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

class TransactionRepository(
    private val api: Api,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val pendingOps: PendingOperationDao,
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

    suspend fun createTransaction(transaction: NewTransaction): Result<Transaction> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = TransactionEntity(
            id = tempId,
            userId = userId,
            accountId = transaction.accountId,
            destinationAccountId = transaction.destinationAccountId,
            type = transaction.type.value,
            status = "completed",
            amount = transaction.amount.toPlainString(),
            currency = transaction.currency,
            categoryId = transaction.categoryId,
            description = transaction.description,
            transactionDate = transaction.transactionDate.toEpochMilli()
        )
        transactionDao.insert(entity)

        // Update account balance locally
        accountDao.getById(transaction.accountId)?.let { account ->
            val currentBalance = account.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val newBalance = when (transaction.type) {
                TransactionType.INCOME -> currentBalance.add(transaction.amount)
                TransactionType.EXPENSE -> currentBalance.subtract(transaction.amount)
                TransactionType.TRANSFER -> currentBalance.subtract(transaction.amount)
            }
            accountDao.insert(account.copy(balance = newBalance.toPlainString()))
        }

        // For transfers, credit the destination account
        if (transaction.type == TransactionType.TRANSFER && transaction.destinationAccountId != null) {
            accountDao.getById(transaction.destinationAccountId)?.let { destAccount ->
                val destBalance = destAccount.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val newDestBalance = destBalance.add(transaction.amount)
                accountDao.insert(destAccount.copy(balance = newDestBalance.toPlainString()))
            }
        }

        val request = transaction.toRequest()
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "transaction",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(
            Transaction(
                id = tempId,
                accountId = transaction.accountId,
                destinationAccountId = transaction.destinationAccountId,
                type = transaction.type,
                status = TransactionStatus.COMPLETED,
                amount = transaction.amount,
                currency = transaction.currency,
                categoryId = transaction.categoryId,
                description = transaction.description,
                transactionDate = transaction.transactionDate
            )
        )
    }

    suspend fun exportCSV(): Result<ByteArray> {
        return api.exportTransactionsCSV()
    }

    // Called by SyncManager only
    suspend fun refreshTransactions(): Result<List<Transaction>> {
        return api.fetchTransactions().map { transactions ->
            val userId = userIdProvider()
            val entities = transactions.map { it.toEntity(userId) }
            transactionDao.deleteAllByUser(userId)
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
}
