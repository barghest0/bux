package com.barghest.bux.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddCategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AddCategoryState>(AddCategoryState.Input)
    val state: StateFlow<AddCategoryState> = _state.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _type = MutableStateFlow(CategoryType.EXPENSE)
    val type: StateFlow<CategoryType> = _type.asStateFlow()

    private val _icon = MutableStateFlow("")
    val icon: StateFlow<String> = _icon.asStateFlow()

    private val _color = MutableStateFlow("#4CAF50")
    val color: StateFlow<String> = _color.asStateFlow()

    fun setName(value: String) {
        _name.value = value
    }

    fun setType(value: CategoryType) {
        _type.value = value
    }

    fun setIcon(value: String) {
        _icon.value = value
    }

    fun setColor(value: String) {
        _color.value = value
    }

    fun createCategory() {
        if (_name.value.isBlank()) {
            _state.value = AddCategoryState.Error("Введите название категории")
            return
        }
        if (_icon.value.isBlank()) {
            _state.value = AddCategoryState.Error("Выберите иконку")
            return
        }

        viewModelScope.launch {
            _state.value = AddCategoryState.Loading
            categoryRepository.createCategory(
                name = _name.value.trim(),
                type = _type.value,
                icon = _icon.value,
                color = _color.value
            ).onSuccess {
                _state.value = AddCategoryState.Success
            }.onFailure { e ->
                _state.value = AddCategoryState.Error(e.message ?: "Failed to create category")
            }
        }
    }

    fun resetState() {
        _state.value = AddCategoryState.Input
    }
}

sealed interface AddCategoryState {
    data object Input : AddCategoryState
    data object Loading : AddCategoryState
    data object Success : AddCategoryState
    data class Error(val message: String) : AddCategoryState
}
