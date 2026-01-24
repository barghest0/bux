package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.CreateTransactionRequest
import com.barghest.bux.data.dto.TransactionResponse
import com.barghest.bux.data.local.entity.TransactionEntity
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionStatus
import com.barghest.bux.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

fun TransactionResponse.toDomain(): Transaction {
    return Transaction(
        id = id,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        type = TransactionType.fromValue(type),
        status = TransactionStatus.fromValue(status),
        amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        currency = currency,
        categoryId = null,
        description = description,
        transactionDate = Instant.parse(transactionDate)
    )
}

fun TransactionResponse.toEntity(userId: Int): TransactionEntity {
    return TransactionEntity(
        id = id,
        userId = userId,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        type = type,
        status = status,
        amount = amount,
        currency = currency,
        categoryId = null,
        description = description,
        transactionDate = Instant.parse(transactionDate).toEpochMilli()
    )
}

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        type = TransactionType.fromValue(type),
        status = TransactionStatus.fromValue(status),
        amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        currency = currency,
        categoryId = categoryId,
        description = description,
        transactionDate = Instant.ofEpochMilli(transactionDate)
    )
}

fun NewTransaction.toRequest(): CreateTransactionRequest {
    return CreateTransactionRequest(
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        type = type.value,
        amount = amount.toPlainString(),
        currency = currency,
        categoryId = categoryId,
        description = description,
        transactionDate = DateTimeFormatter.ISO_INSTANT.format(transactionDate)
    )
}

fun List<TransactionResponse>.toDomainList(): List<Transaction> = map { it.toDomain() }

fun List<TransactionEntity>.toTransactionDomainList(): List<Transaction> = map { it.toDomain() }
