package com.barghest.bux.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.local.PreferencesManager
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.domain.service.AuthService
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authService: AuthService,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        if (tokenManager.isLoggedIn()) {
            uiState = uiState.copy(isLoggedIn = true)
        }
    }

    fun login() {
        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)

            preferencesManager.setOfflineMode(false)

            authService.login(uiState.username, uiState.password)
                .onSuccess {
                    uiState = uiState.copy(loading = false, isLoggedIn = true)
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        loading = false,
                        error = e.message ?: "Неизвестная ошибка"
                    )
                }
        }
    }

    fun loginOffline() {
        preferencesManager.setOfflineMode(true)
        uiState = uiState.copy(isLoggedIn = true)
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
    val isLoggedIn: Boolean = false
)
