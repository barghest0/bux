package com.barghest.bux.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionType
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val service: TransactionService
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val data = service.getTransactions()
            _transactions.value = data
        }
    }
}