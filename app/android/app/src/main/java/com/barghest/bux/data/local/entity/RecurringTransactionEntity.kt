package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["userId"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val accountId: Int,
    val type: String,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val frequency: String,
    val nextDate: Long,
    val endDate: Long?,
    val isActive: Boolean,
    val syncedAt: Long = System.currentTimeMillis()
)
