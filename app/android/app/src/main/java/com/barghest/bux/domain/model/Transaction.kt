package com.barghest.bux.domain.model

data class Transaction(
    val id: Int,
    val amount: Double
)

data class NewTransaction(
    val amount: Double,
    val currency: String
)

enum class TransactionType { INCOME, EXPENSE }


