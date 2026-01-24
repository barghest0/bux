package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.CategoryResponse
import com.barghest.bux.data.local.entity.CategoryEntity
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.CategoryType

fun CategoryResponse.toDomain(): Category = Category(
    id = id,
    name = name,
    type = CategoryType.fromValue(type),
    icon = icon,
    color = color,
    isSystem = isSystem,
    sortOrder = sortOrder
)

fun CategoryResponse.toEntity(userId: Int): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    type = type,
    icon = icon,
    color = color,
    isSystem = isSystem,
    sortOrder = sortOrder
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = CategoryType.fromValue(type),
    icon = icon,
    color = color,
    isSystem = isSystem,
    sortOrder = sortOrder
)

fun List<CategoryResponse>.toDomainList(): List<Category> = map { it.toDomain() }

fun List<CategoryEntity>.toCategoryDomainList(): List<Category> = map { it.toDomain() }
