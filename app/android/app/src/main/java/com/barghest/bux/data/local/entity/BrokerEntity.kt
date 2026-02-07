package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brokers")
data class BrokerEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val name: String,
    val syncedAt: Long = System.currentTimeMillis()
)
