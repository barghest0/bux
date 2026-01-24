package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.LoginRequest
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.User

class AuthRepository(
    private val api: Api,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = api.login(LoginRequest(username, password))
            response.map { dto ->
                // Save token securely
                tokenManager.saveToken(dto.token)
                dto.user.toDomain()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
