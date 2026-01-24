package com.barghest.bux.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AccountsState>(AccountsState.Loading)
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        observeAccounts()
        refresh()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountRepository.getAccountsFlow()
                .onStart { _state.value = AccountsState.Loading }
                .catch { e -> _state.value = AccountsState.Error(e.message ?: "Unknown error") }
                .collect { accounts ->
                    _state.value = if (accounts.isEmpty()) {
                        AccountsState.Empty
                    } else {
                        AccountsState.Success(accounts)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            accountRepository.refreshAccounts()
                .onFailure { e ->
                    if (_state.value is AccountsState.Loading) {
                        _state.value = AccountsState.Error(e.message ?: "Failed to load accounts")
                    }
                }
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            accountRepository.deleteAccount(id)
        }
    }
}

sealed interface AccountsState {
    data object Loading : AccountsState
    data class Success(val accounts: List<Account>) : AccountsState
    data class Error(val message: String) : AccountsState
    data object Empty : AccountsState
}
