package com.barghest.bux.data.mapper

import com.barghest.bux.data.dto.BrokerResponse
import com.barghest.bux.data.dto.HoldingResponse
import com.barghest.bux.data.dto.HoldingWithPnLResponse
import com.barghest.bux.data.dto.PortfolioResponse
import com.barghest.bux.data.dto.PortfolioSummaryResponse
import com.barghest.bux.data.dto.SecurityResponse
import com.barghest.bux.data.dto.TradeResponse
import com.barghest.bux.domain.model.Broker
import com.barghest.bux.domain.model.Holding
import com.barghest.bux.domain.model.HoldingWithPnL
import com.barghest.bux.domain.model.Portfolio
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Security
import com.barghest.bux.domain.model.SecurityType
import com.barghest.bux.domain.model.Trade
import com.barghest.bux.domain.model.TradeSide
import java.math.BigDecimal

fun BrokerResponse.toDomain(): Broker = Broker(
    id = id,
    userId = userId,
    name = name
)

fun PortfolioResponse.toDomain(): Portfolio = Portfolio(
    id = id,
    userId = userId,
    brokerId = brokerId,
    brokerName = broker?.name,
    name = name,
    baseCurrency = baseCurrency
)

fun SecurityResponse.toDomain(): Security = Security(
    id = id,
    symbol = symbol,
    name = name,
    type = SecurityType.fromValue(type),
    currency = currency
)

fun HoldingResponse.toDomain(): Holding = Holding(
    id = id,
    portfolioId = portfolioId,
    securityId = securityId,
    security = security?.toDomain(),
    quantity = quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    averageCost = averageCost.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    totalCost = totalCost.toBigDecimalOrNull() ?: BigDecimal.ZERO
)

fun HoldingWithPnLResponse.toDomain(): HoldingWithPnL = HoldingWithPnL(
    holding = holding.toDomain(),
    currentPrice = currentPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    marketValue = marketValue.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    unrealizedPnL = unrealizedPnL.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    unrealizedPct = unrealizedPct.toBigDecimalOrNull() ?: BigDecimal.ZERO
)

fun PortfolioSummaryResponse.toDomain(): PortfolioSummary = PortfolioSummary(
    portfolioId = portfolioId,
    totalCost = totalCost.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    totalMarketValue = totalMarketValue.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    totalUnrealizedPnL = totalUnrealizedPnL.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    totalUnrealizedPct = totalUnrealizedPct.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    holdings = holdings.map { it.toDomain() }
)

fun TradeResponse.toDomain(): Trade = Trade(
    id = id,
    portfolioId = portfolioId,
    securityId = securityId,
    security = security?.toDomain(),
    tradeDate = tradeDate,
    side = TradeSide.fromValue(side),
    quantity = quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    price = price.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    fee = fee.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    note = note
)

fun List<PortfolioResponse>.toPortfolioDomainList(): List<Portfolio> = map { it.toDomain() }
fun List<BrokerResponse>.toBrokerDomainList(): List<Broker> = map { it.toDomain() }
fun List<TradeResponse>.toTradeDomainList(): List<Trade> = map { it.toDomain() }
fun List<SecurityResponse>.toSecurityDomainList(): List<Security> = map { it.toDomain() }
