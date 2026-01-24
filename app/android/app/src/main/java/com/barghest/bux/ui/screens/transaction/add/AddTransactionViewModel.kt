package com.barghest.bux.ui.screens.transaction.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.NewTransaction
import com.barghest.bux.domain.model.TransactionType
import com.barghest.bux.domain.service.TransactionService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AddTransactionViewModel(
    private val transactionService: TransactionService,
    accountRepository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var uiState by mutableStateOf(AddTransactionUiState())
        private set

    fun updateType(type: TransactionType) {
        uiState = uiState.copy(type = type)
    }

    fun updateAccount(account: Account) {
        uiState = uiState.copy(
            selectedAccount = account,
            currency = account.currency
        )
    }

    fun updateAmount(amount: String) {
        uiState = uiState.copy(amountText = amount)
    }

    fun updateDescription(description: String) {
        uiState = uiState.copy(description = description)
    }

    fun save(onSuccess: () -> Unit) {
        val account = uiState.selectedAccount
        if (account == null) {
            uiState = uiState.copy(error = "Выберите счет")
            return
        }

        val amount = uiState.amountText.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            uiState = uiState.copy(error = "Введите корректную сумму")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)

            val transaction = NewTransaction(
                accountId = account.id,
                type = uiState.type,
                amount = amount,
                currency = uiState.currency,
                description = uiState.description.ifBlank { null }
            )

            transactionService.createTransaction(transaction)
                .onSuccess {
                    uiState = uiState.copy(loading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        loading = false,
                        error = e.message ?: "Ошибка создания транзакции"
                    )
                }
        }
    }
}

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedAccount: Account? = null,
    val amountText: String = "",
    val currency: String = "RUB",
    val description: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
