package com.barghest.bux.data.repository

import com.barghest.bux.data.local.dao.TransactionDao
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toTransactionDomainList
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import com.barghest.bux.domain.model.AssetGroup
import com.barghest.bux.domain.model.CategorySummary
import com.barghest.bux.domain.model.MonthlySummary
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.TransactionSummary
import com.barghest.bux.domain.model.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class AnalyticsRepository(
    private val api: Api,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository,
    private val transactionDao: TransactionDao,
    private val userIdProvider: () -> Int
) {
    /**
     * Compute transaction summary from local Room data.
     * Falls back to API if available, but works fully offline.
     */
    suspend fun getTransactionSummary(from: String? = null, to: String? = null): Result<TransactionSummary> {
        // Try API first for accuracy, fall back to local computation
        val apiResult = api.fetchTransactionSummary(from, to).map { it.toDomain() }
        if (apiResult.isSuccess) return apiResult

        return computeLocalSummary()
    }

    private suspend fun computeLocalSummary(): Result<TransactionSummary> {
        return try {
            val userId = userIdProvider()
            val transactions = transactionDao.getTransactionsByUser(userId).first()
                .toTransactionDomainList()

            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { sum, t -> sum.add(t.amount) }
            val expense = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { sum, t -> sum.add(t.amount) }

            // Group by month
            val byMonth = transactions
                .groupBy {
                    val ld = Instant.ofEpochMilli(it.transactionDate.toEpochMilli())
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    Pair(ld.year, ld.monthValue)
                }
                .map { (key, txs) ->
                    val monthIncome = txs.filter { it.type == TransactionType.INCOME }
                        .fold(BigDecimal.ZERO) { s, t -> s.add(t.amount) }
                    val monthExpense = txs.filter { it.type == TransactionType.EXPENSE }
                        .fold(BigDecimal.ZERO) { s, t -> s.add(t.amount) }
                    MonthlySummary(
                        year = key.first,
                        month = key.second,
                        income = monthIncome,
                        expense = monthExpense,
                        net = monthIncome.subtract(monthExpense)
                    )
                }
                .sortedWith(compareBy({ it.year }, { it.month }))

            Result.success(
                TransactionSummary(
                    totalIncome = income,
                    totalExpense = expense,
                    net = income.subtract(expense),
                    byCategory = emptyList(),
                    byMonth = byMonth
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNetWorth(): Result<NetWorthData> = coroutineScope {
        try {
            val accountsDeferred = async { accountRepository.getAccountsFlow().first() }
            val portfoliosDeferred = async { investmentRepository.getPortfolios() }

            val accounts = accountsDeferred.await()
            val portfolios = portfoliosDeferred.await().getOrDefault(emptyList())

            val summaries = mutableListOf<PortfolioSummary>()
            for (portfolio in portfolios) {
                investmentRepository.getPortfolioSummary(portfolio.id)
                    .onSuccess { summaries.add(it) }
            }

            val assetGroups = accounts
                .filter { it.isActive }
                .groupBy { it.type }
                .map { (type, accs) ->
                    val currency = accs.firstOrNull()?.currency ?: "RUB"
                    AssetGroup(
                        type = type,
                        label = accountTypeLabel(type),
                        totalBalance = accs.fold(BigDecimal.ZERO) { sum, a -> sum.add(a.balance) },
                        currency = currency,
                        accounts = accs
                    )
                }
                .sortedByDescending { it.totalBalance }

            val totalByCurrency = accounts
                .filter { it.isActive }
                .groupBy { it.currency }
                .mapValues { (_, accs) -> accs.fold(BigDecimal.ZERO) { sum, a -> sum.add(a.balance) } }

            val investmentValue = summaries.fold(BigDecimal.ZERO) { sum, s ->
                sum.add(s.totalMarketValue)
            }
            val investmentCurrency = portfolios.firstOrNull()?.baseCurrency ?: "RUB"

            Result.success(
                NetWorthData(
                    totalByCurrency = totalByCurrency,
                    assetGroups = assetGroups,
                    investmentValue = investmentValue,
                    investmentCurrency = investmentCurrency
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.BANK_ACCOUNT -> "Банковские счета"
    AccountType.CARD -> "Карты"
    AccountType.CASH -> "Наличные"
    AccountType.CRYPTO -> "Криптовалюта"
    AccountType.INVESTMENT -> "Инвестиции"
    AccountType.PROPERTY -> "Недвижимость"
}
