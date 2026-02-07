package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.barghest.bux.data.local.entity.TradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades WHERE portfolioId = :portfolioId ORDER BY tradeDate DESC")
    fun getByPortfolio(portfolioId: Int): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getById(id: Int): TradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trades: List<TradeEntity>)

    @Query("DELETE FROM trades WHERE portfolioId = :portfolioId")
    suspend fun deleteByPortfolio(portfolioId: Int)

    @Query("DELETE FROM trades")
    suspend fun deleteAll()
}
