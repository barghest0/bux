package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    @SerialName("is_system") val isSystem: Boolean,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val type: String,
    val icon: String,
    val color: String
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)
