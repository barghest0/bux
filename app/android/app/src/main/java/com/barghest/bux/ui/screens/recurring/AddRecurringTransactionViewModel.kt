package com.barghest.bux.ui.screens.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.data.repository.RecurringTransactionRepository
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.NewRecurringTransaction
import com.barghest.bux.domain.model.RecurrenceFrequency
import com.barghest.bux.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AddRecurringTransactionState(
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedAccount: Account? = null,
    val selectedCategory: Category? = null,
    val amountText: String = "",
    val currency: String = "RUB",
    val description: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val nextDateText: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val loading: Boolean = false,
    val error: String? = null
)

class AddRecurringTransactionViewModel(
    private val recurringRepository: RecurringTransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getActiveAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var uiState by mutableStateOf(AddRecurringTransactionState())
        private set

    fun updateType(type: TransactionType) {
        uiState = uiState.copy(type = type, selectedCategory = null)
    }

    fun updateAccount(account: Account) {
        uiState = uiState.copy(selectedAccount = account, currency = account.currency)
    }

    fun updateCategory(category: Category?) {
        uiState = uiState.copy(selectedCategory = category)
    }

    fun updateAmount(amount: String) {
        uiState = uiState.copy(amountText = amount)
    }

    fun updateDescription(desc: String) {
        uiState = uiState.copy(description = desc)
    }

    fun updateFrequency(freq: RecurrenceFrequency) {
        uiState = uiState.copy(frequency = freq)
    }

    fun updateNextDate(date: String) {
        uiState = uiState.copy(nextDateText = date)
    }

    fun save(onSuccess: () -> Unit) {
        val account = uiState.selectedAccount ?: run {
            uiState = uiState.copy(error = "Выберите счёт")
            return
        }
        val amount = uiState.amountText.toBigDecimalOrNull() ?: run {
            uiState = uiState.copy(error = "Введите корректную сумму")
            return
        }
        val nextDate = try {
            LocalDate.parse(uiState.nextDateText, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()
        } catch (e: Exception) {
            uiState = uiState.copy(error = "Неверный формат даты")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)
            val rt = NewRecurringTransaction(
                accountId = account.id,
                type = uiState.type,
                amount = amount,
                currency = uiState.currency,
                categoryId = uiState.selectedCategory?.id,
                description = uiState.description.ifBlank { null },
                frequency = uiState.frequency,
                nextDate = nextDate
            )
            recurringRepository.create(rt)
                .onSuccess {
                    uiState = uiState.copy(loading = false)
                    onSuccess()
                }
                .onFailure {
                    uiState = uiState.copy(loading = false, error = it.message ?: "Ошибка")
                }
        }
    }
}
