package com.barghest.bux.ui.screens.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.domain.model.AccountType
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AddAccountViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    var uiState by mutableStateOf(AddAccountUiState())
        private set

    fun updateName(name: String) {
        uiState = uiState.copy(name = name)
    }

    fun updateType(type: AccountType) {
        uiState = uiState.copy(type = type)
    }

    fun updateCurrency(currency: String) {
        uiState = uiState.copy(currency = currency)
    }

    fun updateBalance(balance: String) {
        uiState = uiState.copy(balanceText = balance)
    }

    fun updateColor(color: String) {
        uiState = uiState.copy(color = color)
    }

    fun save(onSuccess: () -> Unit) {
        if (uiState.name.isBlank()) {
            uiState = uiState.copy(error = "Введите название счета")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)

            val balance = uiState.balanceText.toBigDecimalOrNull() ?: BigDecimal.ZERO

            accountRepository.createAccount(
                type = uiState.type,
                name = uiState.name,
                currency = uiState.currency,
                balance = balance,
                color = uiState.color.ifBlank { null }
            ).onSuccess {
                uiState = uiState.copy(loading = false)
                onSuccess()
            }.onFailure { e ->
                uiState = uiState.copy(
                    loading = false,
                    error = e.message ?: "Ошибка создания счета"
                )
            }
        }
    }
}

data class AddAccountUiState(
    val name: String = "",
    val type: AccountType = AccountType.BANK_ACCOUNT,
    val currency: String = "RUB",
    val balanceText: String = "",
    val color: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
