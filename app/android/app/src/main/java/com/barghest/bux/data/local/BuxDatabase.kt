package com.barghest.bux.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.local.dao.CategoryDao
import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.local.entity.AccountEntity
import com.barghest.bux.data.local.entity.CategoryEntity
import com.barghest.bux.data.local.entity.TransactionEntity

@Database(
    entities = [AccountEntity::class, TransactionEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BuxDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: BuxDatabase? = null

        fun getDatabase(context: Context): BuxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuxDatabase::class.java,
                    "bux_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
