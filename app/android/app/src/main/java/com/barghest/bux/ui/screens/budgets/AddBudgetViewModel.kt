package com.barghest.bux.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.BudgetRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.domain.model.BudgetPeriod
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class AddBudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AddBudgetState>(AddBudgetState.Input)
    val state: StateFlow<AddBudgetState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _period = MutableStateFlow(BudgetPeriod.MONTHLY)
    val period: StateFlow<BudgetPeriod> = _period.asStateFlow()

    private val _currency = MutableStateFlow("RUB")
    val currency: StateFlow<String> = _currency.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByTypeFlow(CategoryType.EXPENSE).collect {
                _categories.value = it
            }
        }
    }

    fun setCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun setAmount(value: String) {
        _amount.value = value
    }

    fun setPeriod(value: BudgetPeriod) {
        _period.value = value
    }

    fun setCurrency(value: String) {
        _currency.value = value
    }

    fun createBudget() {
        val category = _selectedCategory.value
        if (category == null) {
            _state.value = AddBudgetState.Error("Выберите категорию")
            return
        }
        val amountDecimal = try {
            BigDecimal(_amount.value)
        } catch (e: Exception) {
            _state.value = AddBudgetState.Error("Введите корректную сумму")
            return
        }
        if (amountDecimal <= BigDecimal.ZERO) {
            _state.value = AddBudgetState.Error("Сумма должна быть больше нуля")
            return
        }

        viewModelScope.launch {
            _state.value = AddBudgetState.Loading
            budgetRepository.createBudget(
                categoryId = category.id,
                amount = amountDecimal,
                currency = _currency.value,
                period = _period.value.value
            ).onSuccess {
                _state.value = AddBudgetState.Success
            }.onFailure { e ->
                _state.value = AddBudgetState.Error(e.message ?: "Не удалось создать бюджет")
            }
        }
    }

    fun resetState() {
        _state.value = AddBudgetState.Input
    }
}

sealed interface AddBudgetState {
    data object Input : AddBudgetState
    data object Loading : AddBudgetState
    data object Success : AddBudgetState
    data class Error(val message: String) : AddBudgetState
}
