package com.barghest.bux.data.repository

import android.util.Log
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toRequest
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction

class TransactionRepository(
    private val api: Api
) {
    suspend fun getAll(): List<Transaction> {
        val result = api.fetchTransactions()

        return result.fold(
            onSuccess = { list ->
                Log.d(
                    "TransactionRepo", "Success: getAll $list"
                )
                list.map { it.toDomain() }

            },
            onFailure = { error ->
                Log.e("TransactionRepo", "Failed to fetch transactions: ${error.message}", error)
                emptyList()
            }
        )
    }

    suspend fun add(transaction: NewTransaction): Result<Unit> {
        val result = api.postTransaction(transaction.toRequest())
        return result.onSuccess {
            Log.d("Transaction", "Транзакция успешно добавлена")
        }.onFailure { error ->
            Log.e("Transaction", "Ошибка при добавлении транзакции: ${error.message}", error)
        }
    }
}