package com.barghest.bux.ui.screens.transaction.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val service: TransactionService
) : ViewModel() {

    fun addTransaction(transaction: NewTransaction) {
        viewModelScope.launch {
            service.addTransaction(transaction)
        }
    }
}