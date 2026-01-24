package com.barghest.bux.domain.model

import java.math.BigDecimal

data class Account(
    val id: Int,
    val type: AccountType,
    val name: String,
    val currency: String,
    val balance: BigDecimal,
    val icon: String?,
    val color: String?,
    val isActive: Boolean,
    val sortOrder: Int
)

enum class AccountType(val value: String) {
    BANK_ACCOUNT("bank_account"),
    CARD("card"),
    CASH("cash"),
    CRYPTO("crypto"),
    INVESTMENT("investment"),
    PROPERTY("property");

    companion object {
        fun fromValue(value: String): AccountType {
            return entries.find { it.value == value } ?: BANK_ACCOUNT
        }
    }
}
