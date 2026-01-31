package com.barghest.bux.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.RecurringTransactionRepository
import com.barghest.bux.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecurringTransactionsState(
    val items: List<RecurringTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecurringTransactionsViewModel(
    private val repository: RecurringTransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringTransactionsState())
    val state: StateFlow<RecurringTransactionsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getFlow().collect { items ->
                _state.value = _state.value.copy(items = items)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repository.refresh()
                .onFailure { _state.value = _state.value.copy(error = it.message) }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun toggleActive(id: Int) {
        viewModelScope.launch {
            repository.toggleActive(id)
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
