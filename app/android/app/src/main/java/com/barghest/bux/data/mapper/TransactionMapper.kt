package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.TransactionRequest
import com.barghest.bux.data.dto.TransactionResponse
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.Transaction

fun TransactionResponse.toDomain(): Transaction {
    return Transaction(
        id = id,
        amount = amount
    )
}

fun NewTransaction.toRequest(): TransactionRequest {
    return TransactionRequest(
        amount = amount,
        currency = currency
    )
}