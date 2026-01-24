package com.barghest.bux.ui.screens.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.domain.model.Portfolio
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Trade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortfolioDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val portfolioId: Int = savedStateHandle.get<Int>("portfolioId") ?: 0

    private val _state = MutableStateFlow<PortfolioDetailState>(PortfolioDetailState.Loading)
    val state: StateFlow<PortfolioDetailState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadPortfolioDetails()
    }

    fun loadPortfolioDetails() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val portfolioResult = investmentRepository.getPortfolio(portfolioId)
                val summaryResult = investmentRepository.getPortfolioSummary(portfolioId)
                val tradesResult = investmentRepository.getTrades(portfolioId)

                portfolioResult.onSuccess { portfolio ->
                    val summary = summaryResult.getOrNull()
                    val trades = tradesResult.getOrNull() ?: emptyList()

                    _state.value = PortfolioDetailState.Success(
                        portfolio = portfolio,
                        summary = summary,
                        trades = trades
                    )
                }.onFailure { e ->
                    _state.value = PortfolioDetailState.Error(e.message ?: "Failed to load portfolio")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        loadPortfolioDetails()
    }
}

sealed interface PortfolioDetailState {
    data object Loading : PortfolioDetailState
    data class Success(
        val portfolio: Portfolio,
        val summary: PortfolioSummary?,
        val trades: List<Trade>
    ) : PortfolioDetailState
    data class Error(val message: String) : PortfolioDetailState
}
