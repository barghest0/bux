package com.barghest.bux.domain.model

import java.math.BigDecimal
import java.time.Instant

data class Transaction(
    val id: Int,
    val accountId: Int,
    val destinationAccountId: Int?,
    val type: TransactionType,
    val status: TransactionStatus,
    val amount: BigDecimal,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val transactionDate: Instant
)

data class NewTransaction(
    val accountId: Int,
    val destinationAccountId: Int? = null,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val categoryId: Int? = null,
    val description: String? = null,
    val transactionDate: Instant = Instant.now()
)

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer");

    companion object {
        fun fromValue(value: String): TransactionType {
            return entries.find { it.value == value } ?: EXPENSE
        }
    }
}

enum class TransactionStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        fun fromValue(value: String): TransactionStatus {
            return entries.find { it.value == value } ?: COMPLETED
        }
    }
}
