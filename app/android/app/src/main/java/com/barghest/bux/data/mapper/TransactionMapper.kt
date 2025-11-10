package com.barghest.bux.data.mapper

import com.barghest.bux.data.model.TransactionDto
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionType

fun TransactionDto.toDomain(): Transaction {
    return Transaction(
        id = id,
        amount = amount
    )
}