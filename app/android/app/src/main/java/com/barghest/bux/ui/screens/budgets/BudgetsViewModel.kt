package com.barghest.bux.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.BudgetRepository
import com.barghest.bux.domain.model.BudgetStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BudgetsState {
    data object Loading : BudgetsState
    data class Success(val statuses: List<BudgetStatus>) : BudgetsState
    data class Error(val message: String) : BudgetsState
}

class BudgetsViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BudgetsState>(BudgetsState.Loading)
    val state: StateFlow<BudgetsState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadDataInternal()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteBudget(id: Int) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
            loadDataInternal()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = BudgetsState.Loading
            loadDataInternal()
        }
    }

    private suspend fun loadDataInternal() {
        budgetRepository.getBudgetStatus()
            .onSuccess { _state.value = BudgetsState.Success(it) }
            .onFailure { _state.value = BudgetsState.Error(it.message ?: "Unknown error") }
    }
}
