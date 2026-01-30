package com.barghest.bux.domain.model

import java.math.BigDecimal

data class TransactionSummary(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val net: BigDecimal,
    val byCategory: List<CategorySummary>,
    val byMonth: List<MonthlySummary>
)

data class CategorySummary(
    val categoryId: Int?,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val type: String,
    val total: BigDecimal,
    val count: Int
)

data class MonthlySummary(
    val year: Int,
    val month: Int,
    val income: BigDecimal,
    val expense: BigDecimal,
    val net: BigDecimal
)

data class AssetGroup(
    val type: AccountType,
    val label: String,
    val totalBalance: BigDecimal,
    val currency: String,
    val accounts: List<Account>
)

data class NetWorthData(
    val totalByCurrency: Map<String, BigDecimal>,
    val assetGroups: List<AssetGroup>,
    val investmentValue: BigDecimal,
    val investmentCurrency: String
)
