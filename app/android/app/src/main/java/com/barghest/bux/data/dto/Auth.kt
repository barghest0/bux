package com.barghest.bux.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoggedInUser(
    val id: Int,
    val username: String
)

@Serializable
data class LoginResponse(
    val user: LoggedInUser,
    val token: String
)