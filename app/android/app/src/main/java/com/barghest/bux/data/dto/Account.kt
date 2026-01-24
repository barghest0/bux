package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    val id: Int,
    val type: String,
    val name: String,
    val currency: String,
    val balance: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CreateAccountRequest(
    val type: String,
    val name: String,
    val currency: String,
    val balance: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class UpdateAccountRequest(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)
