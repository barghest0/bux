package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateBrokerRequest
import com.barghest.bux.data.dto.CreatePortfolioRequest
import com.barghest.bux.data.dto.CreateTradeRequest
import com.barghest.bux.data.mapper.toBrokerDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toPortfolioDomainList
import com.barghest.bux.data.mapper.toSecurityDomainList
import com.barghest.bux.data.mapper.toTradeDomainList
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Broker
import com.barghest.bux.domain.model.NewTrade
import com.barghest.bux.domain.model.Portfolio
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Security
import com.barghest.bux.domain.model.Trade

class InvestmentRepository(
    private val api: Api
) {
    // Brokers
    suspend fun getBrokers(): Result<List<Broker>> {
        return api.fetchBrokers().map { it.toBrokerDomainList() }
    }

    suspend fun createBroker(name: String): Result<Broker> {
        val request = CreateBrokerRequest(
            name = name
        )
        return api.createBroker(request).map { it.toDomain() }
    }

    // Portfolios
    suspend fun getPortfolios(): Result<List<Portfolio>> {
        return api.fetchPortfolios().map { it.toPortfolioDomainList() }
    }

    suspend fun getPortfolio(id: Int): Result<Portfolio> {
        return api.fetchPortfolio(id).map { it.toDomain() }
    }

    suspend fun getPortfolioSummary(portfolioId: Int): Result<PortfolioSummary> {
        return api.fetchPortfolioSummary(portfolioId).map { it.toDomain() }
    }

    suspend fun createPortfolio(
        brokerId: Int,
        name: String,
        baseCurrency: String = "RUB"
    ): Result<Portfolio> {
        val request = CreatePortfolioRequest(
            brokerId = brokerId,
            name = name,
            baseCurrency = baseCurrency
        )
        return api.createPortfolio(request).map { it.toDomain() }
    }

    // Trades
    suspend fun getTrades(portfolioId: Int): Result<List<Trade>> {
        return api.fetchTrades(portfolioId).map { it.toTradeDomainList() }
    }

    suspend fun createTrade(trade: NewTrade): Result<Trade> {
        val request = CreateTradeRequest(
            portfolioId = trade.portfolioId,
            securityId = trade.securityId,
            side = trade.side.value,
            quantity = trade.quantity.toPlainString(),
            price = trade.price.toPlainString(),
            fee = trade.fee.toPlainString(),
            tradeDate = trade.tradeDate,
            note = trade.note
        )
        return api.createTrade(request).map { it.toDomain() }
    }

    // Securities
    suspend fun searchSecurities(query: String, type: String? = null): Result<List<Security>> {
        return api.searchSecurities(query, type).map { it.toSecurityDomainList() }
    }

    suspend fun getSecurity(id: Int): Result<Security> {
        return api.fetchSecurity(id).map { it.toDomain() }
    }
}
