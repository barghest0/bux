package com.barghest.bux.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "securities")
data class SecurityEntity(
    @PrimaryKey val id: Int,
    val symbol: String,
    val name: String,
    val type: String,
    val currency: String,
    val syncedAt: Long = System.currentTimeMillis()
)
