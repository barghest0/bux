package com.barghest.bux.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.TopCategory
import com.barghest.bux.domain.model.TrendItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class InsightsState(
    val trends: List<TrendItem> = emptyList(),
    val topCategories: List<TopCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class InsightsViewModel(
    private val api: Api
) : ViewModel() {
    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Fetch trends
            api.fetchTrends(6)
                .onSuccess { response ->
                    val trends = response.trends.map { t ->
                        TrendItem(
                            year = t.year,
                            month = t.month,
                            income = t.income.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            expense = t.expense.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            net = t.net.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            prevIncome = t.prevIncome?.toBigDecimalOrNull(),
                            prevExpense = t.prevExpense?.toBigDecimalOrNull()
                        )
                    }
                    _state.value = _state.value.copy(trends = trends)
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }

            // Fetch top categories
            api.fetchTopCategories("expense")
                .onSuccess { response ->
                    val categories = response.categories.map { c ->
                        TopCategory(
                            categoryId = c.categoryId,
                            categoryName = c.categoryName,
                            categoryIcon = c.categoryIcon,
                            categoryColor = c.categoryColor,
                            total = c.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            count = c.count
                        )
                    }
                    _state.value = _state.value.copy(topCategories = categories)
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }

            _state.value = _state.value.copy(isLoading = false)
        }
    }
}
