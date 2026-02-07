package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateCategoryRequest
import com.barghest.bux.data.dto.UpdateCategoryRequest
import com.barghest.bux.data.local.dao.CategoryDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.entity.CategoryEntity
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.mapper.toCategoryDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepository(
    private val api: Api,
    private val categoryDao: CategoryDao,
    private val pendingOps: PendingOperationDao,
    private val userIdProvider: () -> Int
) {
    fun getCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getCategoriesByUser(userIdProvider()).map { it.toCategoryDomainList() }
    }

    fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByUserAndType(userIdProvider(), type.value)
            .map { it.toCategoryDomainList() }
    }

    suspend fun getCategory(id: Int): Category? {
        return categoryDao.getById(id)?.toDomain()
    }

    suspend fun createCategory(
        name: String,
        type: CategoryType,
        icon: String,
        color: String
    ): Result<Category> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = CategoryEntity(
            id = tempId,
            userId = userId,
            name = name,
            type = type.value,
            icon = icon,
            color = color,
            isSystem = false,
            sortOrder = 0
        )
        categoryDao.insert(entity)

        val request = CreateCategoryRequest(name = name, type = type.value, icon = icon, color = color)
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "category",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    suspend fun updateCategory(
        id: Int,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        sortOrder: Int? = null
    ): Result<Category> {
        val existing = categoryDao.getById(id) ?: return Result.failure(Exception("Not found"))
        val updated = existing.copy(
            name = name ?: existing.name,
            icon = icon ?: existing.icon,
            color = color ?: existing.color,
            sortOrder = sortOrder ?: existing.sortOrder
        )
        categoryDao.insert(updated)

        val request = UpdateCategoryRequest(name = name, icon = icon, color = color, sortOrder = sortOrder)
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "category",
                entityId = id,
                operationType = "update",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(updated.toDomain())
    }

    suspend fun deleteCategory(id: Int): Result<Unit> {
        categoryDao.getById(id)?.let { categoryDao.delete(it) }

        pendingOps.insert(
            PendingOperationEntity(
                entityType = "category",
                entityId = id,
                operationType = "delete",
                payload = ""
            )
        )

        return Result.success(Unit)
    }

    // Called by SyncManager only
    suspend fun refreshCategories(): Result<List<Category>> {
        return api.fetchCategories().map { categories ->
            val userId = userIdProvider()
            val entities = categories.map { it.toEntity(userId) }
            categoryDao.deleteAllByUser(userId)
            categoryDao.insertAll(entities)
            categories.map { it.toDomain() }
        }
    }
}
