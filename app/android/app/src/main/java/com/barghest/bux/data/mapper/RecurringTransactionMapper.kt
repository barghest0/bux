package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.CreateRecurringTransactionRequest
import com.barghest.bux.data.dto.RecurringTransactionResponse
import com.barghest.bux.data.local.entity.RecurringTransactionEntity
import com.barghest.bux.domain.model.NewRecurringTransaction
import com.barghest.bux.domain.model.RecurringTransaction
import com.barghest.bux.domain.model.RecurrenceFrequency
import com.barghest.bux.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter

fun RecurringTransactionResponse.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = id,
        accountId = accountId,
        type = TransactionType.fromValue(type),
        amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        currency = currency,
        categoryId = category?.id,
        description = description,
        frequency = RecurrenceFrequency.fromValue(frequency),
        nextDate = Instant.parse(nextDate),
        endDate = endDate?.let { Instant.parse(it) },
        isActive = isActive
    )
}

fun RecurringTransactionResponse.toEntity(userId: Int): RecurringTransactionEntity {
    return RecurringTransactionEntity(
        id = id,
        userId = userId,
        accountId = accountId,
        type = type,
        amount = amount,
        currency = currency,
        categoryId = category?.id,
        description = description,
        frequency = frequency,
        nextDate = Instant.parse(nextDate).toEpochMilli(),
        endDate = endDate?.let { Instant.parse(it).toEpochMilli() },
        isActive = isActive
    )
}

fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    return RecurringTransaction(
        id = id,
        accountId = accountId,
        type = TransactionType.fromValue(type),
        amount = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        currency = currency,
        categoryId = categoryId,
        description = description,
        frequency = RecurrenceFrequency.fromValue(frequency),
        nextDate = Instant.ofEpochMilli(nextDate),
        endDate = endDate?.let { Instant.ofEpochMilli(it) },
        isActive = isActive
    )
}

fun NewRecurringTransaction.toRequest(): CreateRecurringTransactionRequest {
    return CreateRecurringTransactionRequest(
        accountId = accountId,
        type = type.value,
        amount = amount.toPlainString(),
        currency = currency,
        categoryId = categoryId,
        description = description,
        frequency = frequency.value,
        nextDate = DateTimeFormatter.ISO_INSTANT.format(nextDate),
        endDate = endDate?.let { DateTimeFormatter.ISO_INSTANT.format(it) }
    )
}

fun List<RecurringTransactionResponse>.toRecurringDomainList(): List<RecurringTransaction> = map { it.toDomain() }
fun List<RecurringTransactionEntity>.toRecurringEntityDomainList(): List<RecurringTransaction> = map { it.toDomain() }
