package com.barghest.bux.domain.model

data class User(
    val id: Int,
    val username: String,
    val email: String? = null
)
