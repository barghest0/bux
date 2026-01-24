package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
        Index(value = ["userId"]),
        Index(value = ["transactionDate"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val accountId: Int,
    val destinationAccountId: Int?,
    val type: String,
    val status: String,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val description: String?,
    val transactionDate: Long,
    val syncedAt: Long = System.currentTimeMillis()
)
