package com.barghest.bux.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.AnalyticsRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.MonthlySummary
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.TransactionSummary
import com.barghest.bux.domain.service.TransactionService
import com.barghest.bux.domain.model.Transaction
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed interface DashboardState {
    data object Loading : DashboardState
    data class Success(
        val netWorth: NetWorthData,
        val summary: TransactionSummary?,
        val recentTransactions: List<Transaction>,
        val accounts: List<Account>
    ) : DashboardState
    data class Error(val message: String) : DashboardState
}

class AnalyticsDashboardViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                accountRepository.refreshAccounts()
                transactionService.refreshTransactions()
                loadDataInternal()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            loadDataInternal()
        }
    }

    private suspend fun loadDataInternal() {
        try {
            val netWorthResult = analyticsRepository.getNetWorth()
            val summaryResult = analyticsRepository.getTransactionSummary()
            val transactions = transactionService.getTransactionsFlow().first()
            val accounts = accountRepository.getAccountsFlow().first()

            val netWorth = netWorthResult.getOrElse {
                _state.value = DashboardState.Error(it.message ?: "Failed to load net worth")
                return
            }

            _state.value = DashboardState.Success(
                netWorth = netWorth,
                summary = summaryResult.getOrNull(),
                recentTransactions = transactions.take(5),
                accounts = accounts
            )
        } catch (e: Exception) {
            _state.value = DashboardState.Error(e.message ?: "Unknown error")
        }
    }
}
