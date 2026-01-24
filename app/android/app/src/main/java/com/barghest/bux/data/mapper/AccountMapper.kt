package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.AccountResponse
import com.barghest.bux.data.local.entity.AccountEntity
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import java.math.BigDecimal

fun AccountResponse.toDomain(): Account = Account(
    id = id,
    type = AccountType.fromValue(type),
    name = name,
    currency = currency,
    balance = balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    icon = icon,
    color = color,
    isActive = isActive,
    sortOrder = sortOrder
)

fun AccountResponse.toEntity(userId: Int): AccountEntity = AccountEntity(
    id = id,
    userId = userId,
    type = type,
    name = name,
    currency = currency,
    balance = balance,
    icon = icon,
    color = color,
    isActive = isActive,
    sortOrder = sortOrder
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    type = AccountType.fromValue(type),
    name = name,
    currency = currency,
    balance = balance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    icon = icon,
    color = color,
    isActive = isActive,
    sortOrder = sortOrder
)

fun List<AccountResponse>.toDomainList(): List<Account> = map { it.toDomain() }

fun List<AccountEntity>.toAccountDomainList(): List<Account> = map { it.toDomain() }
