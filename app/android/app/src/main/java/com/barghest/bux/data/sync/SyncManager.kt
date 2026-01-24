package com.barghest.bux.data.sync

import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.data.repository.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SyncManager(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun syncAll(): Result<SyncResult> = coroutineScope {
        try {
            val categoriesDeferred = async { categoryRepository.refreshCategories() }
            val accountsDeferred = async { accountRepository.refreshAccounts() }

            val categoriesResult = categoriesDeferred.await()
            val accountsResult = accountsDeferred.await()

            // Transactions depend on accounts, so fetch after
            val transactionsResult = transactionRepository.refreshTransactions()

            val errors = mutableListOf<String>()
            categoriesResult.onFailure { errors.add("Categories: ${it.message}") }
            accountsResult.onFailure { errors.add("Accounts: ${it.message}") }
            transactionsResult.onFailure { errors.add("Transactions: ${it.message}") }

            if (errors.isEmpty()) {
                Result.success(SyncResult.Success)
            } else {
                Result.success(SyncResult.PartialSuccess(errors))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncCategories(): Result<Unit> {
        return categoryRepository.refreshCategories().map { }
    }

    suspend fun syncAccounts(): Result<Unit> {
        return accountRepository.refreshAccounts().map { }
    }

    suspend fun syncTransactions(): Result<Unit> {
        return transactionRepository.refreshTransactions().map { }
    }
}

sealed interface SyncResult {
    data object Success : SyncResult
    data class PartialSuccess(val errors: List<String>) : SyncResult
}
