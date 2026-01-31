package com.barghest.bux.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionType
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface MainScreenState {
    data object Loading : MainScreenState
    data class Success(
        val accounts: List<Account>,
        val recentTransactions: List<Transaction>,
        val categories: List<Category>,
        val searchQuery: String,
        val filters: TransactionFilterState
    ) : MainScreenState
    data class Error(val message: String) : MainScreenState
}

class MainViewModel(
    private val transactionService: TransactionService,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filters = MutableStateFlow(TransactionFilterState())
    val filters: StateFlow<TransactionFilterState> = _filters.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                accountRepository.getAccountsFlow(),
                transactionService.getTransactionsFlow(),
                categoryRepository.getCategoriesFlow(),
                _searchQuery,
                _filters
            ) { accounts, transactions, categories, query, filters ->
                val filtered = applyFilters(transactions, query, filters)
                MainScreenState.Success(
                    accounts = accounts,
                    recentTransactions = filtered,
                    categories = categories,
                    searchQuery = query,
                    filters = filters
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

    fun updateSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun updateFilters(update: (TransactionFilterState) -> TransactionFilterState) {
        _filters.value = update(_filters.value)
    }

    fun clearFilters() {
        _filters.value = TransactionFilterState()
    }

    private fun applyFilters(
        transactions: List<Transaction>,
        query: String,
        filters: TransactionFilterState
    ): List<Transaction> {
        if (transactions.isEmpty()) return transactions

        val normalizedQuery = query.trim().lowercase()
        val fromDate = parseDate(filters.fromDate)
        val toDate = parseDate(filters.toDate)
        val minAmount = filters.minAmount.toBigDecimalOrNull()
        val maxAmount = filters.maxAmount.toBigDecimalOrNull()

        return transactions.filter { tx ->
            if (normalizedQuery.isNotEmpty()) {
                val description = tx.description.orEmpty().lowercase()
                if (!description.contains(normalizedQuery)) return@filter false
            }

            if (filters.type != null && tx.type != filters.type) return@filter false
            if (filters.categoryId != null && tx.categoryId != filters.categoryId) return@filter false

            if (fromDate != null || toDate != null) {
                val txDate = LocalDate.ofInstant(tx.transactionDate, ZoneId.systemDefault())
                if (fromDate != null && txDate.isBefore(fromDate)) return@filter false
                if (toDate != null && txDate.isAfter(toDate)) return@filter false
            }

            if (minAmount != null && tx.amount < minAmount) return@filter false
            if (maxAmount != null && tx.amount > maxAmount) return@filter false

            true
        }
    }

    private fun parseDate(value: String): LocalDate? {
        if (value.isBlank()) return null
        return try {
            LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data class Success(val file: File) : ExportState
    data class Error(val message: String) : ExportState
}

data class TransactionFilterState(
    val type: TransactionType? = null,
    val categoryId: Int? = null,
    val fromDate: String = "",
    val toDate: String = "",
    val minAmount: String = "",
    val maxAmount: String = ""
)
