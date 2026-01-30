package com.barghest.bux.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AnalyticsRepository
import com.barghest.bux.domain.model.NetWorthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NetWorthState {
    data object Loading : NetWorthState
    data class Success(val data: NetWorthData) : NetWorthState
    data class Error(val message: String) : NetWorthState
}

class NetWorthViewModel(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NetWorthState>(NetWorthState.Loading)
    val state: StateFlow<NetWorthState> = _state.asStateFlow()

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

    private fun loadData() {
        viewModelScope.launch {
            _state.value = NetWorthState.Loading
            loadDataInternal()
        }
    }

    private suspend fun loadDataInternal() {
        analyticsRepository.getNetWorth()
            .onSuccess { _state.value = NetWorthState.Success(it) }
            .onFailure { _state.value = NetWorthState.Error(it.message ?: "Unknown error") }
    }
}
