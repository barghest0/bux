package com.barghest.bux.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.local.dao.BrokerDao
import com.barghest.bux.data.local.dao.BudgetDao
import com.barghest.bux.data.local.dao.CategoryDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.dao.PortfolioDao
import com.barghest.bux.data.local.dao.RecurringTransactionDao
import com.barghest.bux.data.local.dao.SecurityDao
import com.barghest.bux.data.local.dao.TradeDao
import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.local.entity.AccountEntity
import com.barghest.bux.data.local.entity.BrokerEntity
import com.barghest.bux.data.local.entity.BudgetEntity
import com.barghest.bux.data.local.entity.CategoryEntity
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.local.entity.PortfolioEntity
import com.barghest.bux.data.local.entity.RecurringTransactionEntity
import com.barghest.bux.data.local.entity.SecurityEntity
import com.barghest.bux.data.local.entity.TradeEntity
import com.barghest.bux.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class,
        BudgetEntity::class,
        PortfolioEntity::class,
        TradeEntity::class,
        SecurityEntity::class,
        BrokerEntity::class,
        PendingOperationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BuxDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun tradeDao(): TradeDao
    abstract fun securityDao(): SecurityDao
    abstract fun brokerDao(): BrokerDao
    abstract fun pendingOperationDao(): PendingOperationDao

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
