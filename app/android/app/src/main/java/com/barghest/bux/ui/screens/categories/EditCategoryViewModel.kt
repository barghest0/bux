package com.barghest.bux.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditCategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EditCategoryState>(EditCategoryState.Loading)
    val state: StateFlow<EditCategoryState> = _state.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _icon = MutableStateFlow("")
    val icon: StateFlow<String> = _icon.asStateFlow()

    private val _color = MutableStateFlow("#4CAF50")
    val color: StateFlow<String> = _color.asStateFlow()

    private val _isSystem = MutableStateFlow(false)
    val isSystem: StateFlow<Boolean> = _isSystem.asStateFlow()

    private var loadedCategoryId: Int? = null
    private var loadedCategory: Category? = null

    fun loadCategory(categoryId: Int) {
        if (loadedCategoryId == categoryId) return
        loadedCategoryId = categoryId

        viewModelScope.launch {
            _state.value = EditCategoryState.Loading
            val category = categoryRepository.getCategory(categoryId)
            if (category == null) {
                _state.value = EditCategoryState.Error("Категория не найдена")
                return@launch
            }
            _isSystem.value = category.isSystem
            loadedCategory = category
            _name.value = category.name
            _icon.value = category.icon
            _color.value = category.color
            _state.value = EditCategoryState.Input(category)
        }
    }

    fun setName(value: String) {
        _name.value = value
    }

    fun setIcon(value: String) {
        _icon.value = value
    }

    fun setColor(value: String) {
        _color.value = value
    }

    fun updateCategory() {
        val categoryId = loadedCategoryId ?: return
        if (_isSystem.value) {
            _state.value = EditCategoryState.Error("Системную категорию нельзя редактировать")
            return
        }
        if (_name.value.isBlank()) {
            _state.value = EditCategoryState.Error("Введите название категории")
            return
        }
        if (_icon.value.isBlank()) {
            _state.value = EditCategoryState.Error("Выберите иконку")
            return
        }

        viewModelScope.launch {
            _state.value = EditCategoryState.Saving
            categoryRepository.updateCategory(
                id = categoryId,
                name = _name.value.trim(),
                icon = _icon.value,
                color = _color.value
            ).onSuccess { updated ->
                loadedCategory = updated
                _state.value = EditCategoryState.Success(updated)
            }.onFailure { e ->
                _state.value = EditCategoryState.Error(e.message ?: "Не удалось обновить категорию")
            }
        }
    }

    fun resetState() {
        loadedCategory?.let {
            _state.value = EditCategoryState.Input(it)
        }
    }
}

sealed interface EditCategoryState {
    data object Loading : EditCategoryState
    data class Input(val category: Category) : EditCategoryState
    data object Saving : EditCategoryState
    data class Success(val category: Category) : EditCategoryState
    data class Error(val message: String) : EditCategoryState
}
