package com.barghest.bux.domain.model

import java.math.BigDecimal
import java.time.Instant

data class RecurringTransaction(
    val id: Int,
    val accountId: Int,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val frequency: RecurrenceFrequency,
    val nextDate: Instant,
    val endDate: Instant?,
    val isActive: Boolean
)

data class NewRecurringTransaction(
    val accountId: Int,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val categoryId: Int? = null,
    val description: String? = null,
    val frequency: RecurrenceFrequency,
    val nextDate: Instant,
    val endDate: Instant? = null
)

enum class RecurrenceFrequency(val value: String, val label: String) {
    DAILY("daily", "Ежедневно"),
    WEEKLY("weekly", "Еженедельно"),
    MONTHLY("monthly", "Ежемесячно"),
    YEARLY("yearly", "Ежегодно");

    companion object {
        fun fromValue(value: String): RecurrenceFrequency {
            return entries.find { it.value == value } ?: MONTHLY
        }
    }
}
