package com.barghest.bux.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CategoriesState>(CategoriesState.Loading)
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    private val _selectedType = MutableStateFlow(CategoryType.EXPENSE)
    val selectedType: StateFlow<CategoryType> = _selectedType.asStateFlow()

    init {
        observeCategories()
        refresh()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesFlow()
                .onStart { _state.value = CategoriesState.Loading }
                .catch { e -> _state.value = CategoriesState.Error(e.message ?: "Unknown error") }
                .collect { categories ->
                    _state.value = if (categories.isEmpty()) {
                        CategoriesState.Empty
                    } else {
                        CategoriesState.Success(categories)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            categoryRepository.refreshCategories()
                .onFailure { e ->
                    if (_state.value is CategoriesState.Loading) {
                        _state.value = CategoriesState.Error(e.message ?: "Failed to load categories")
                    }
                }
        }
    }

    fun selectType(type: CategoryType) {
        _selectedType.value = type
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
        }
    }
}

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Success(val categories: List<Category>) : CategoriesState
    data class Error(val message: String) : CategoriesState
    data object Empty : CategoriesState
}
