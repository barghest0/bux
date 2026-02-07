package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateAccountRequest
import com.barghest.bux.data.dto.UpdateAccountRequest
import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.entity.AccountEntity
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.mapper.toAccountDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class AccountRepository(
    private val api: Api,
    private val accountDao: AccountDao,
    private val pendingOps: PendingOperationDao,
    private val userIdProvider: () -> Int
) {
    fun getAccountsFlow(): Flow<List<Account>> {
        return accountDao.getAccountsByUser(userIdProvider()).map { it.toAccountDomainList() }
    }

    fun getActiveAccountsFlow(): Flow<List<Account>> {
        return accountDao.getActiveAccountsByUser(userIdProvider()).map { it.toAccountDomainList() }
    }

    suspend fun getAccount(id: Int): Account? {
        return accountDao.getById(id)?.toDomain()
    }

    suspend fun createAccount(
        type: AccountType,
        name: String,
        currency: String,
        balance: BigDecimal? = null,
        icon: String? = null,
        color: String? = null
    ): Result<Account> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = AccountEntity(
            id = tempId,
            userId = userId,
            type = type.value,
            name = name,
            currency = currency,
            balance = balance?.toPlainString() ?: "0",
            icon = icon,
            color = color,
            isActive = true,
            sortOrder = 0
        )
        accountDao.insert(entity)

        val request = CreateAccountRequest(
            type = type.value,
            name = name,
            currency = currency,
            balance = balance?.toPlainString(),
            icon = icon,
            color = color
        )
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "account",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    suspend fun updateAccount(
        id: Int,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        sortOrder: Int? = null
    ): Result<Account> {
        val existing = accountDao.getById(id) ?: return Result.failure(Exception("Not found"))
        val updated = existing.copy(
            name = name ?: existing.name,
            icon = icon ?: existing.icon,
            color = color ?: existing.color,
            sortOrder = sortOrder ?: existing.sortOrder
        )
        accountDao.insert(updated)

        val request = UpdateAccountRequest(name = name, icon = icon, color = color, sortOrder = sortOrder)
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "account",
                entityId = id,
                operationType = "update",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(updated.toDomain())
    }

    suspend fun deleteAccount(id: Int): Result<Unit> {
        accountDao.getById(id)?.let { accountDao.delete(it) }

        pendingOps.insert(
            PendingOperationEntity(
                entityType = "account",
                entityId = id,
                operationType = "delete",
                payload = ""
            )
        )

        return Result.success(Unit)
    }

    // Called by SyncManager only
    suspend fun refreshAccounts(): Result<List<Account>> {
        return api.fetchAccounts().map { accounts ->
            val userId = userIdProvider()
            val entities = accounts.map { it.toEntity(userId) }
            accountDao.deleteAllByUser(userId)
            accountDao.insertAll(entities)
            accounts.map { it.toDomain() }
        }
    }
}
