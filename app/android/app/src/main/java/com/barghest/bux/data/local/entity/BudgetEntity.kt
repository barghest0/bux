package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val categoryId: Int,
    val categoryName: String?,
    val amount: String,
    val currency: String,
    val period: String,
    val syncedAt: Long = System.currentTimeMillis()
)
