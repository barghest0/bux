package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trades",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["portfolioId"])]
)
data class TradeEntity(
    @PrimaryKey val id: Int,
    val portfolioId: Int,
    val securityId: Int,
    val securitySymbol: String?,
    val securityName: String?,
    val securityType: String?,
    val securityCurrency: String?,
    val tradeDate: String,
    val side: String,
    val quantity: String,
    val price: String,
    val fee: String,
    val note: String?,
    val syncedAt: Long = System.currentTimeMillis()
)
