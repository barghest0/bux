package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.barghest.bux.data.local.entity.BrokerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao {
    @Query("SELECT * FROM brokers WHERE userId = :userId ORDER BY name ASC")
    fun getByUser(userId: Int): Flow<List<BrokerEntity>>

    @Query("SELECT * FROM brokers WHERE id = :id")
    suspend fun getById(id: Int): BrokerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(broker: BrokerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(brokers: List<BrokerEntity>)

    @Query("DELETE FROM brokers WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Int)
}
