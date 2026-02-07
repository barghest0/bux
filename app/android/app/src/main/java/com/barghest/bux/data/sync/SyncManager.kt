package com.barghest.bux.data.sync

import android.util.Log
import com.barghest.bux.data.dto.CreateAccountRequest
import com.barghest.bux.data.dto.CreateBudgetRequest
import com.barghest.bux.data.dto.CreateCategoryRequest
import com.barghest.bux.data.dto.CreatePortfolioRequest
import com.barghest.bux.data.dto.CreateRecurringTransactionRequest
import com.barghest.bux.data.dto.CreateTradeRequest
import com.barghest.bux.data.dto.UpdateAccountRequest
import com.barghest.bux.data.dto.UpdateCategoryRequest
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.BudgetRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.data.repository.RecurringTransactionRepository
import com.barghest.bux.data.repository.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class SyncManager(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val investmentRepository: InvestmentRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val pendingOps: PendingOperationDao,
    private val api: Api
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Full sync: push pending local changes, then pull fresh data from server.
     */
    suspend fun syncAll(): Result<SyncResult> = coroutineScope {
        try {
            // Phase 1: Push pending operations to server
            pushPendingOperations()

            // Phase 2: Pull fresh data from server
            val errors = mutableListOf<String>()

            val categoriesDeferred = async { categoryRepository.refreshCategories() }
            val accountsDeferred = async { accountRepository.refreshAccounts() }
            val budgetsDeferred = async { budgetRepository.refreshBudgets() }
            val portfoliosDeferred = async { investmentRepository.refreshPortfolios() }
            val brokersDeferred = async { investmentRepository.refreshBrokers() }
            val recurringDeferred = async { recurringRepository.refresh() }

            categoriesDeferred.await().onFailure { errors.add("Categories: ${it.message}") }
            accountsDeferred.await().onFailure { errors.add("Accounts: ${it.message}") }
            budgetsDeferred.await().onFailure { errors.add("Budgets: ${it.message}") }
            portfoliosDeferred.await().onFailure { errors.add("Portfolios: ${it.message}") }
            brokersDeferred.await().onFailure { errors.add("Brokers: ${it.message}") }
            recurringDeferred.await().onFailure { errors.add("Recurring: ${it.message}") }

            // Transactions depend on accounts
            transactionRepository.refreshTransactions()
                .onFailure { errors.add("Transactions: ${it.message}") }

            // Refresh trades for each portfolio
            val portfolios = investmentRepository.getPortfolios().getOrDefault(emptyList())
            for (p in portfolios) {
                investmentRepository.refreshTrades(p.id)
                    .onFailure { errors.add("Trades(${p.id}): ${it.message}") }
            }

            if (errors.isEmpty()) {
                Result.success(SyncResult.Success)
            } else {
                Result.success(SyncResult.PartialSuccess(errors))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pushPendingOperations() {
        val ops = pendingOps.getAll()
        for (op in ops) {
            val success = processOperation(op)
            if (success) {
                pendingOps.deleteById(op.id)
            } else {
                pendingOps.incrementRetry(op.id)
            }
        }
        pendingOps.deleteStale(10)
    }

    private suspend fun processOperation(op: PendingOperationEntity): Boolean {
        return try {
            when (op.entityType) {
                "account" -> processAccountOp(op)
                "transaction" -> processTransactionOp(op)
                "category" -> processCategoryOp(op)
                "budget" -> processBudgetOp(op)
                "portfolio" -> processPortfolioOp(op)
                "trade" -> processTradeOp(op)
                "broker" -> processBrokerOp(op)
                "recurring_transaction" -> processRecurringOp(op)
                else -> {
                    Log.w("SyncManager", "Unknown entity type: ${op.entityType}")
                    true // Remove unknown ops
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to process op ${op.id}: ${e.message}")
            false
        }
    }

    private suspend fun processAccountOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreateAccountRequest>(op.payload)
                api.createAccount(req).isSuccess
            }
            "update" -> {
                val req = json.decodeFromString<UpdateAccountRequest>(op.payload)
                api.updateAccount(op.entityId!!, req).isSuccess
            }
            "delete" -> api.deleteAccount(op.entityId!!).isSuccess
            else -> true
        }
    }

    private suspend fun processTransactionOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<com.barghest.bux.data.dto.CreateTransactionRequest>(op.payload)
                api.createTransaction(req).isSuccess
            }
            else -> true
        }
    }

    private suspend fun processCategoryOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreateCategoryRequest>(op.payload)
                api.createCategory(req).isSuccess
            }
            "update" -> {
                val req = json.decodeFromString<UpdateCategoryRequest>(op.payload)
                api.updateCategory(op.entityId!!, req).isSuccess
            }
            "delete" -> api.deleteCategory(op.entityId!!).isSuccess
            else -> true
        }
    }

    private suspend fun processBudgetOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreateBudgetRequest>(op.payload)
                api.createBudget(req).isSuccess
            }
            "delete" -> api.deleteBudget(op.entityId!!).isSuccess
            else -> true
        }
    }

    private suspend fun processPortfolioOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreatePortfolioRequest>(op.payload)
                api.createPortfolio(req).isSuccess
            }
            else -> true
        }
    }

    private suspend fun processTradeOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreateTradeRequest>(op.payload)
                api.createTrade(req).isSuccess
            }
            else -> true
        }
    }

    private suspend fun processBrokerOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<com.barghest.bux.data.dto.CreateBrokerRequest>(op.payload)
                api.createBroker(req).isSuccess
            }
            else -> true
        }
    }

    private suspend fun processRecurringOp(op: PendingOperationEntity): Boolean {
        return when (op.operationType) {
            "create" -> {
                val req = json.decodeFromString<CreateRecurringTransactionRequest>(op.payload)
                api.createRecurringTransaction(req).isSuccess
            }
            "toggle" -> api.toggleRecurringTransaction(op.entityId!!).isSuccess
            "execute" -> api.executeRecurringTransaction(op.entityId!!).isSuccess
            "delete" -> api.deleteRecurringTransaction(op.entityId!!).isSuccess
            else -> true
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
