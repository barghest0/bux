package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateCategoryRequest
import com.barghest.bux.data.dto.UpdateCategoryRequest
import com.barghest.bux.data.local.dao.CategoryDao
import com.barghest.bux.data.mapper.toCategoryDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val api: Api,
    private val categoryDao: CategoryDao,
    private val userIdProvider: () -> Int
) {
    fun getCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getCategoriesByUser(userIdProvider()).map { it.toCategoryDomainList() }
    }

    fun getCategoriesByTypeFlow(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByUserAndType(userIdProvider(), type.value)
            .map { it.toCategoryDomainList() }
    }

    suspend fun refreshCategories(): Result<List<Category>> {
        return api.fetchCategories().map { categories ->
            val userId = userIdProvider()
            val entities = categories.map { it.toEntity(userId) }
            categoryDao.insertAll(entities)
            categories.map { it.toDomain() }
        }
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
        val request = CreateCategoryRequest(
            name = name,
            type = type.value,
            icon = icon,
            color = color
        )
        return api.createCategory(request).map { response ->
            val userId = userIdProvider()
            categoryDao.insert(response.toEntity(userId))
            response.toDomain()
        }
    }

    suspend fun updateCategory(
        id: Int,
        name: String? = null,
        icon: String? = null,
        color: String? = null,
        sortOrder: Int? = null
    ): Result<Category> {
        val request = UpdateCategoryRequest(
            name = name,
            icon = icon,
            color = color,
            sortOrder = sortOrder
        )
        return api.updateCategory(id, request).map { response ->
            val userId = userIdProvider()
            categoryDao.insert(response.toEntity(userId))
            response.toDomain()
        }
    }

    suspend fun deleteCategory(id: Int): Result<Unit> {
        return api.deleteCategory(id).also {
            if (it.isSuccess) {
                categoryDao.getById(id)?.let { entity ->
                    categoryDao.delete(entity)
                }
            }
        }
    }
}
