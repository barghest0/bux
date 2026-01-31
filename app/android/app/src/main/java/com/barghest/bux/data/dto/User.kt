package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val email: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val username: String,
    val email: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String
)
