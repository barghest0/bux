package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateAccountRequest
import com.barghest.bux.data.dto.UpdateAccountRequest
import com.barghest.bux.data.local.dao.AccountDao
import com.barghest.bux.data.mapper.toAccountDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class AccountRepository(
    private val api: Api,
    private val accountDao: AccountDao,
    private val userIdProvider: () -> Int
) {
    fun getAccountsFlow(): Flow<List<Account>> {
        return accountDao.getAccountsByUser(userIdProvider()).map { it.toAccountDomainList() }
    }

    fun getActiveAccountsFlow(): Flow<List<Account>> {
        return accountDao.getActiveAccountsByUser(userIdProvider()).map { it.toAccountDomainList() }
    }

    suspend fun refreshAccounts(): Result<List<Account>> {
        return api.fetchAccounts().map { accounts ->
            val userId = userIdProvider()
            val entities = accounts.map { it.toEntity(userId) }
            accountDao.insertAll(entities)
            accounts.map { it.toDomain() }
        }
    }

    suspend fun getAccount(id: Int): Result<Account> {
        return api.fetchAccount(id).map { it.toDomain() }
    }

    suspend fun createAccount(
        type: AccountType,
        name: String,
        currency: String,
        balance: BigDecimal? = null,
        icon: String? = null,
        color: String? = null
    ): Result<Account> {
        val request = CreateAccountRequest(
            type = type.value,
            name = name,
            currency = currency,
            balance = balance?.toPlainString(),
            icon = icon,
            color = color
        )
        return api.createAccount(request).map { response ->
            val userId = userIdProvider()
            accountDao.insert(response.toEntity(userId))
            response.toDomain()
        }
    }

    suspend fun updateAccount(
        id: Int,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        sortOrder: Int? = null
    ): Result<Account> {
        val request = UpdateAccountRequest(
            name = name,
            icon = icon,
            color = color,
            sortOrder = sortOrder
        )
        return api.updateAccount(id, request).map { response ->
            val userId = userIdProvider()
            accountDao.insert(response.toEntity(userId))
            response.toDomain()
        }
    }

    suspend fun deleteAccount(id: Int): Result<Unit> {
        return api.deleteAccount(id).also {
            if (it.isSuccess) {
                accountDao.getById(id)?.let { entity ->
                    accountDao.delete(entity)
                }
            }
        }
    }
}
