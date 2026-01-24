package com.barghest.bux.domain.model

data class Category(
    val id: Int,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val isSystem: Boolean,
    val sortOrder: Int
)

enum class CategoryType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromValue(value: String): CategoryType {
            return entries.find { it.value == value } ?: EXPENSE
        }
    }
}
