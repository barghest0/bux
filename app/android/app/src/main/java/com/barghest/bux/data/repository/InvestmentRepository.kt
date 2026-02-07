package com.barghest.bux.data.repository

import com.barghest.bux.data.dto.CreateBrokerRequest
import com.barghest.bux.data.dto.CreatePortfolioRequest
import com.barghest.bux.data.dto.CreateTradeRequest
import com.barghest.bux.data.local.dao.BrokerDao
import com.barghest.bux.data.local.dao.PendingOperationDao
import com.barghest.bux.data.local.dao.PortfolioDao
import com.barghest.bux.data.local.dao.SecurityDao
import com.barghest.bux.data.local.dao.TradeDao
import com.barghest.bux.data.local.entity.BrokerEntity
import com.barghest.bux.data.local.entity.PendingOperationEntity
import com.barghest.bux.data.local.entity.PortfolioEntity
import com.barghest.bux.data.mapper.toBrokerDomainList
import com.barghest.bux.data.mapper.toBrokerEntityDomainList
import com.barghest.bux.data.mapper.toDomain
import com.barghest.bux.data.mapper.toEntity
import com.barghest.bux.data.mapper.toPortfolioDomainList
import com.barghest.bux.data.mapper.toPortfolioEntityDomainList
import com.barghest.bux.data.mapper.toSecurityDomainList
import com.barghest.bux.data.mapper.toSecurityEntityDomainList
import com.barghest.bux.data.mapper.toTradeEntityDomainList
import com.barghest.bux.data.mapper.toTradeDomainList
import com.barghest.bux.data.network.Api
import com.barghest.bux.domain.model.Broker
import com.barghest.bux.domain.model.NewTrade
import com.barghest.bux.domain.model.Portfolio
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Security
import com.barghest.bux.domain.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InvestmentRepository(
    private val api: Api,
    private val portfolioDao: PortfolioDao,
    private val tradeDao: TradeDao,
    private val securityDao: SecurityDao,
    private val brokerDao: BrokerDao,
    private val pendingOps: PendingOperationDao,
    private val userIdProvider: () -> Int
) {
    // ── Portfolios ──

    fun getPortfoliosFlow(): Flow<List<Portfolio>> {
        return portfolioDao.getByUser(userIdProvider()).map { it.toPortfolioEntityDomainList() }
    }

    suspend fun getPortfolios(): Result<List<Portfolio>> {
        val local = portfolioDao.getByUser(userIdProvider()).first()
        return Result.success(local.toPortfolioEntityDomainList())
    }

    suspend fun getPortfolio(id: Int): Result<Portfolio> {
        val entity = portfolioDao.getById(id)
            ?: return Result.failure(Exception("Portfolio not found"))
        return Result.success(entity.toDomain())
    }

    suspend fun getPortfolioSummary(portfolioId: Int): Result<PortfolioSummary> {
        // Summary requires market prices — try API, fall back to empty
        return api.fetchPortfolioSummary(portfolioId).map { it.toDomain() }
    }

    suspend fun createPortfolio(
        brokerId: Int,
        name: String,
        baseCurrency: String = "RUB"
    ): Result<Portfolio> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val broker = brokerDao.getById(brokerId)
        val entity = PortfolioEntity(
            id = tempId,
            userId = userId,
            brokerId = brokerId,
            brokerName = broker?.name,
            name = name,
            baseCurrency = baseCurrency
        )
        portfolioDao.insert(entity)

        val request = CreatePortfolioRequest(brokerId = brokerId, name = name, baseCurrency = baseCurrency)
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "portfolio",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    // ── Trades ──

    fun getTradesFlow(portfolioId: Int): Flow<List<Trade>> {
        return tradeDao.getByPortfolio(portfolioId).map { it.toTradeEntityDomainList() }
    }

    suspend fun getTrades(portfolioId: Int): Result<List<Trade>> {
        val local = tradeDao.getByPortfolio(portfolioId).first()
        return Result.success(local.toTradeEntityDomainList())
    }

    suspend fun createTrade(trade: NewTrade): Result<Trade> {
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val security = securityDao.getById(trade.securityId)
        val entity = com.barghest.bux.data.local.entity.TradeEntity(
            id = tempId,
            portfolioId = trade.portfolioId,
            securityId = trade.securityId,
            securitySymbol = security?.symbol,
            securityName = security?.name,
            securityType = security?.type,
            securityCurrency = security?.currency,
            tradeDate = trade.tradeDate,
            side = trade.side.value,
            quantity = trade.quantity.toPlainString(),
            price = trade.price.toPlainString(),
            fee = trade.fee.toPlainString(),
            note = trade.note
        )
        tradeDao.insert(entity)

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
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "trade",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    // ── Brokers ──

    fun getBrokersFlow(): Flow<List<Broker>> {
        return brokerDao.getByUser(userIdProvider()).map { it.toBrokerEntityDomainList() }
    }

    suspend fun getBrokers(): Result<List<Broker>> {
        val local = brokerDao.getByUser(userIdProvider()).first()
        return Result.success(local.toBrokerEntityDomainList())
    }

    suspend fun createBroker(name: String): Result<Broker> {
        val userId = userIdProvider()
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val entity = BrokerEntity(id = tempId, userId = userId, name = name)
        brokerDao.insert(entity)

        val request = CreateBrokerRequest(name = name)
        pendingOps.insert(
            PendingOperationEntity(
                entityType = "broker",
                entityId = tempId,
                operationType = "create",
                payload = Json.encodeToString(request)
            )
        )

        return Result.success(entity.toDomain())
    }

    // ── Securities ──

    suspend fun searchSecurities(query: String, type: String? = null): Result<List<Security>> {
        // Try local first
        val local = securityDao.search(query)
        if (local.isNotEmpty()) {
            return Result.success(local.toSecurityEntityDomainList())
        }
        // Fall back to API
        return api.searchSecurities(query, type).map { list ->
            securityDao.insertAll(list.map { it.toEntity() })
            list.toSecurityDomainList()
        }
    }

    suspend fun getSecurity(id: Int): Result<Security> {
        securityDao.getById(id)?.let { return Result.success(it.toDomain()) }
        return api.fetchSecurity(id).map { response ->
            securityDao.insert(response.toEntity())
            response.toDomain()
        }
    }

    // ── Sync (called by SyncManager) ──

    suspend fun refreshPortfolios(): Result<List<Portfolio>> {
        return api.fetchPortfolios().map { list ->
            val userId = userIdProvider()
            portfolioDao.deleteAllByUser(userId)
            portfolioDao.insertAll(list.map { it.toEntity(userId) })
            list.toPortfolioDomainList()
        }
    }

    suspend fun refreshTrades(portfolioId: Int): Result<List<Trade>> {
        return api.fetchTrades(portfolioId).map { list ->
            tradeDao.deleteByPortfolio(portfolioId)
            tradeDao.insertAll(list.map { it.toEntity() })
            list.toTradeDomainList()
        }
    }

    suspend fun refreshBrokers(): Result<List<Broker>> {
        return api.fetchBrokers().map { list ->
            val userId = userIdProvider()
            brokerDao.deleteAllByUser(userId)
            brokerDao.insertAll(list.map { it.toEntity(userId) })
            list.toBrokerDomainList()
        }
    }
}
