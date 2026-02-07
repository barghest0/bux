package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.barghest.bux.data.local.entity.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolios WHERE userId = :userId ORDER BY id DESC")
    fun getByUser(userId: Int): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolios WHERE id = :id")
    suspend fun getById(id: Int): PortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(portfolio: PortfolioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(portfolios: List<PortfolioEntity>)

    @Query("DELETE FROM portfolios WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM portfolios WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Int)
}
