package com.barghest.bux.ui.screens.accounts

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch

class AccountDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService
) : ViewModel() {

    private val accountId: Int = savedStateHandle.get<Int>("accountId") ?: 0

    private val _state = MutableStateFlow<AccountDetailState>(AccountDetailState.Loading)
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    init {
        observeAccount()
        refresh()
    }

    private fun observeAccount() {
        viewModelScope.launch {
            combine(
                accountRepository.getAccountsFlow(),
                transactionService.getTransactionsByAccountFlow(accountId)
            ) { accounts, transactions ->
                Pair(accounts.firstOrNull { it.id == accountId }, transactions)
            }.catch { e ->
                _state.value = AccountDetailState.Error(e.message ?: "Не удалось загрузить счет")
            }.collect { (account, transactions) ->
                _state.value = if (account == null) {
                    AccountDetailState.NotFound
                } else {
                    AccountDetailState.Success(
                        account = account,
                        transactions = transactions
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val accountResult = accountRepository.refreshAccounts()
                val transactionResult = transactionService.refreshTransactionsByAccount(accountId)
                if (_state.value is AccountDetailState.Loading &&
                    accountResult.isFailure && transactionResult.isFailure
                ) {
                    _state.value = AccountDetailState.Error("Не удалось обновить данные счета")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        if (_isDeleting.value) return

        viewModelScope.launch {
            _isDeleting.value = true
            try {
                accountRepository.deleteAccount(accountId)
                    .onSuccess { onSuccess() }
                    .onFailure { e ->
                        _state.value = AccountDetailState.Error(
                            e.message ?: "Не удалось удалить счет"
                        )
                    }
            } finally {
                _isDeleting.value = false
            }
        }
    }
}

sealed interface AccountDetailState {
    data object Loading : AccountDetailState
    data object NotFound : AccountDetailState
    data class Success(
        val account: Account,
        val transactions: List<Transaction>
    ) : AccountDetailState
    data class Error(val message: String) : AccountDetailState
}
