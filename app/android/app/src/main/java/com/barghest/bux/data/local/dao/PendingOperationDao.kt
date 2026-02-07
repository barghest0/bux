package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.barghest.bux.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun countFlow(): Flow<Int>

    @Insert
    suspend fun insert(op: PendingOperationEntity): Long

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_operations SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM pending_operations WHERE retryCount > :maxRetries")
    suspend fun deleteStale(maxRetries: Int = 10)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()
}
