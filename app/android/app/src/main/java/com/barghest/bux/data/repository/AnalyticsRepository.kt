package com.barghest.bux.data.repository

import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import com.barghest.bux.domain.model.AssetGroup
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.TransactionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

class AnalyticsRepository(
    private val api: Api,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository
) {
    suspend fun getTransactionSummary(from: String? = null, to: String? = null): Result<TransactionSummary> {
        return api.fetchTransactionSummary(from, to).map { it.toDomain() }
    }

    suspend fun getNetWorth(): Result<NetWorthData> = coroutineScope {
        try {
            val accountsDeferred = async { accountRepository.getAccountsFlow().first() }
            val portfoliosDeferred = async { investmentRepository.getPortfolios() }

            val accounts = accountsDeferred.await()
            val portfolios = portfoliosDeferred.await().getOrDefault(emptyList())

            // Get portfolio summaries for market values
            val summaries = mutableListOf<PortfolioSummary>()
            for (portfolio in portfolios) {
                investmentRepository.getPortfolioSummary(portfolio.id)
                    .onSuccess { summaries.add(it) }
            }

            // Group accounts by type
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

            // Total by currency from accounts
            val totalByCurrency = accounts
                .filter { it.isActive }
                .groupBy { it.currency }
                .mapValues { (_, accs) -> accs.fold(BigDecimal.ZERO) { sum, a -> sum.add(a.balance) } }

            // Total investment market value
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
