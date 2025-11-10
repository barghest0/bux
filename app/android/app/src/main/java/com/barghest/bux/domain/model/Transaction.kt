package com.barghest.bux.domain.model

data class Transaction(
    val id: Int,
    val amount: Double
)

enum class TransactionType { INCOME, EXPENSE }