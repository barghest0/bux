package com.barghest.bux.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.local.entity.AccountEntity
import com.barghest.bux.data.local.entity.TransactionEntity

@Database(
    entities = [AccountEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BuxDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BuxDatabase? = null

        fun getDatabase(context: Context): BuxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuxDatabase::class.java,
                    "bux_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
