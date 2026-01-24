package com.barghest.bux.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.barghest.bux.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByUser(userId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByUserAndType(userId: Int, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE userId = :userId AND isSystem = 0")
    suspend fun deleteCustomByUser(userId: Int)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Int)
}
