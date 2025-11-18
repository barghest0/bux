package com.barghest.bux.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.domain.model.User
import com.barghest.bux.domain.service.AuthService
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun login() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)

            val result = authService.login(uiState.username, uiState.password)

            result
                .onSuccess { (user, token) ->
                    uiState = uiState.copy(
                        loading = false,
                        user = user,
                        token = token
                    )
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        loading = false,
                        error = e.message ?: "Неизвестная ошибка"
                    )
                }
        }
    }

    fun updateUsername(value: String) {
        uiState = uiState.copy(username = value)
    }

    fun updatePassword(value: String) {
        uiState = uiState.copy(password = value)
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val token: String? = null
)
