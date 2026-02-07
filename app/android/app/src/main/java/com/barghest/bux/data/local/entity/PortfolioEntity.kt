package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolios")
data class PortfolioEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val brokerId: Int,
    val brokerName: String?,
    val name: String,
    val baseCurrency: String,
    val syncedAt: Long = System.currentTimeMillis()
)
