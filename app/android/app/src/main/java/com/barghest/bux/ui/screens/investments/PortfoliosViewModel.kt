package com.barghest.bux.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.domain.model.Portfolio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortfoliosViewModel(
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PortfoliosState>(PortfoliosState.Loading)
    val state: StateFlow<PortfoliosState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadPortfolios()
    }

    fun loadPortfolios() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                investmentRepository.getPortfolios()
                    .onSuccess { portfolios ->
                        _state.value = if (portfolios.isEmpty()) {
                            PortfoliosState.Empty
                        } else {
                            PortfoliosState.Success(portfolios)
                        }
                    }
                    .onFailure { e ->
                        _state.value = PortfoliosState.Error(e.message ?: "Failed to load portfolios")
                    }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        loadPortfolios()
    }
}

sealed interface PortfoliosState {
    data object Loading : PortfoliosState
    data class Success(val portfolios: List<Portfolio>) : PortfoliosState
    data class Error(val message: String) : PortfoliosState
    data object Empty : PortfoliosState
}
