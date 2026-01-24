package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val name: String,
    val type: String, // "income" or "expense"
    val icon: String,
    val color: String,
    val isSystem: Boolean,
    val sortOrder: Int,
    val syncedAt: Long = System.currentTimeMillis()
)
