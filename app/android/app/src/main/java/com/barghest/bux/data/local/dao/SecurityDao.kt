package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.barghest.bux.data.local.entity.SecurityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    @Query("SELECT * FROM securities ORDER BY symbol ASC")
    fun getAll(): Flow<List<SecurityEntity>>

    @Query("SELECT * FROM securities WHERE id = :id")
    suspend fun getById(id: Int): SecurityEntity?

    @Query("SELECT * FROM securities WHERE symbol LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<SecurityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(security: SecurityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(securities: List<SecurityEntity>)

    @Query("DELETE FROM securities")
    suspend fun deleteAll()
}
