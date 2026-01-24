package com.barghest.bux.domain.service

import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.domain.model.User

class AuthService(
    private val repo: AuthRepository
) {
    suspend fun login(username: String, password: String): Result<User> {
        return repo.login(username, password)
    }

    fun logout() {
        repo.logout()
    }

    fun isLoggedIn(): Boolean = repo.isLoggedIn()
}
