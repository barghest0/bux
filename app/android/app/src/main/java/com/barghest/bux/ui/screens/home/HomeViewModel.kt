package com.barghest.bux.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.AnalyticsRepository
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.Portfolio
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionSummary
import com.barghest.bux.domain.model.TrendItem
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class PortfolioWithSummary(
    val portfolio: Portfolio,
    val summary: PortfolioSummary?
)

data class InsightMessage(
    val icon: String,
    val text: String
)

sealed interface HomeState {
    data object Loading : HomeState
    data class Success(
        val netWorth: NetWorthData,
        val netWorthChange: BigDecimal?,
        val accounts: List<Account>,
        val portfolios: List<PortfolioWithSummary>,
        val summary: TransactionSummary?,
        val recentTransactions: List<Transaction>,
        val insight: InsightMessage?
    ) : HomeState
    data class Error(val message: String) : HomeState
}

class HomeViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository,
    private val transactionService: TransactionService,
    private val api: Api
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

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
            _state.value = HomeState.Loading
            loadDataInternal()
        }
    }

    private suspend fun loadDataInternal() {
        try {
            val netWorthDeferred = viewModelScope.async { analyticsRepository.getNetWorth() }
            val summaryDeferred = viewModelScope.async { analyticsRepository.getTransactionSummary() }
            val portfoliosDeferred = viewModelScope.async { investmentRepository.getPortfolios() }
            val trendsDeferred = viewModelScope.async { api.fetchTrends(2) }

            val transactions = transactionService.getTransactionsFlow().first()
            val accounts = accountRepository.getAccountsFlow().first()

            val netWorth = netWorthDeferred.await().getOrElse {
                _state.value = HomeState.Error(it.message ?: "Failed to load")
                return
            }

            val summary = summaryDeferred.await().getOrNull()
            val portfolios = portfoliosDeferred.await().getOrDefault(emptyList())

            // Load portfolio summaries
            val portfolioWithSummaries = portfolios.map { portfolio ->
                val s = investmentRepository.getPortfolioSummary(portfolio.id).getOrNull()
                PortfolioWithSummary(portfolio, s)
            }

            // Compute net worth change from trends
            val trends = trendsDeferred.await().getOrNull()?.trends
            val netWorthChange = computeNetWorthChange(summary)

            // Generate insight from trends
            val insight = generateInsight(trends, summary)

            _state.value = HomeState.Success(
                netWorth = netWorth,
                netWorthChange = netWorthChange,
                accounts = accounts,
                portfolios = portfolioWithSummaries,
                summary = summary,
                recentTransactions = transactions.take(5),
                insight = insight
            )
        } catch (e: Exception) {
            _state.value = HomeState.Error(e.message ?: "Unknown error")
        }
    }

    private fun computeNetWorthChange(summary: TransactionSummary?): BigDecimal? {
        if (summary == null) return null
        val months = summary.byMonth
        if (months.size < 2) return null
        val current = months.last()
        val previous = months[months.size - 2]
        val currentNet = current.income.subtract(current.expense)
        val previousNet = previous.income.subtract(previous.expense)
        if (previousNet.compareTo(BigDecimal.ZERO) == 0) return null
        return currentNet.subtract(previousNet)
            .multiply(BigDecimal(100))
            .divide(previousNet.abs(), 1, RoundingMode.HALF_UP)
    }

    private fun generateInsight(
        trends: List<com.barghest.bux.data.dto.TrendItemResponse>?,
        summary: TransactionSummary?
    ): InsightMessage? {
        if (summary != null && summary.net > BigDecimal.ZERO) {
            return InsightMessage(
                icon = "\uD83D\uDCC8",
                text = "В этом периоде вы заработали больше, чем потратили. Отличный результат!"
            )
        }

        if (trends != null && trends.size >= 2) {
            val current = trends.last()
            val prev = trends[trends.size - 2]
            val currentExpense = current.expense.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val prevExpense = prev.expense.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (prevExpense > BigDecimal.ZERO) {
                val change = currentExpense.subtract(prevExpense)
                    .multiply(BigDecimal(100))
                    .divide(prevExpense, 0, RoundingMode.HALF_UP)
                if (change > BigDecimal(10)) {
                    return InsightMessage(
                        icon = "\uD83D\uDCA1",
                        text = "Расходы выросли на ${change.toInt()}% по сравнению с прошлым месяцем"
                    )
                }
            }
        }

        return null
    }
}
