package com.barghest.bux.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import android.content.Context
import java.io.File

sealed interface MainScreenState {
    data object Loading : MainScreenState
    data class Success(
        val accounts: List<Account>,
        val recentTransactions: List<Transaction>
    ) : MainScreenState
    data class Error(val message: String) : MainScreenState
}

class MainViewModel(
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                accountRepository.getAccountsFlow(),
                transactionService.getTransactionsFlow()
            ) { accounts, transactions ->
                MainScreenState.Success(
                    accounts = accounts,
                    recentTransactions = transactions.take(10)
                )
            }
                .onStart { _state.value = MainScreenState.Loading }
                .catch { e -> _state.value = MainScreenState.Error(e.message ?: "Unknown error") }
                .collect { _state.value = it }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                accountRepository.refreshAccounts()
                transactionService.refreshTransactions()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun exportCSV(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            transactionService.exportCSV()
                .onSuccess { bytes ->
                    val file = File(context.cacheDir, "transactions.csv")
                    file.writeBytes(bytes)
                    _exportState.value = ExportState.Success(file)
                }
                .onFailure { e ->
                    _exportState.value = ExportState.Error(e.message ?: "Export failed")
                }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data class Success(val file: File) : ExportState
    data class Error(val message: String) : ExportState
}
