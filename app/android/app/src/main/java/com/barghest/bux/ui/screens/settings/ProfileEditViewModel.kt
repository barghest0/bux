package com.barghest.bux.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditState(
    val username: String = "",
    val email: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileEditViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authRepository.getProfile()
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        username = user.username,
                        email = user.email.orEmpty(),
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить профиль"
                    )
                }
        }
    }

    fun updateUsername(value: String) {
        _state.value = _state.value.copy(username = value)
    }

    fun updateEmail(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    fun updateCurrentPassword(value: String) {
        _state.value = _state.value.copy(currentPassword = value)
    }

    fun updateNewPassword(value: String) {
        _state.value = _state.value.copy(newPassword = value)
    }

    fun updateConfirmPassword(value: String) {
        _state.value = _state.value.copy(confirmPassword = value)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authRepository.updateProfile(
                username = _state.value.username,
                email = _state.value.email.ifBlank { null }
            )
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        username = user.username,
                        email = user.email.orEmpty(),
                        isLoading = false,
                        successMessage = "Профиль обновлён"
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось обновить профиль"
                    )
                }
        }
    }

    fun updatePassword() {
        val current = _state.value.currentPassword
        val newPassword = _state.value.newPassword
        val confirm = _state.value.confirmPassword

        if (newPassword.isBlank() || current.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Заполните все поля пароля")
            return
        }
        if (newPassword != confirm) {
            _state.value = _state.value.copy(errorMessage = "Пароли не совпадают")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authRepository.updatePassword(current, newPassword)
                .onSuccess {
                    _state.value = _state.value.copy(
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        isLoading = false,
                        successMessage = "Пароль обновлён"
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось обновить пароль"
                    )
                }
        }
    }
}
