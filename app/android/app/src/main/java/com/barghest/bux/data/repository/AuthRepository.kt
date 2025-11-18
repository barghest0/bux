package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.LoginRequest
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.User

class AuthRepository(
    private val api: Api
) {
    suspend fun login(username: String, password: String): Result<Pair<User, String>> {
        return try {
            val response = api.login(LoginRequest(username, password))
            response.map { dto ->
                dto.user.toDomain() to dto.token
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}