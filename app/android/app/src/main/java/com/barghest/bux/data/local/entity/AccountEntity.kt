package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val type: String,
    val name: String,
    val currency: String,
    val balance: String,
    val icon: String?,
    val color: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val syncedAt: Long = System.currentTimeMillis()
)
