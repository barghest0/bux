package com.barghest.bux.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TransactionListState {
    data object Loading : TransactionListState
    data class Success(val transactions: List<Transaction>) : TransactionListState
    data class Error(val message: String) : TransactionListState
    data object Empty : TransactionListState
}

class MainViewModel(
    private val service: TransactionService
) : ViewModel() {

    private val _state = MutableStateFlow<TransactionListState>(TransactionListState.Loading)
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = TransactionListState.Loading

            service.getTransactions()
                .onSuccess { transactions ->
                    _state.value = if (transactions.isEmpty()) {
                        TransactionListState.Empty
                    } else {
                        TransactionListState.Success(transactions)
                    }
                }
                .onFailure { error ->
                    _state.value = TransactionListState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }
}
