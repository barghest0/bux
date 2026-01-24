package com.barghest.bux.domain.model

import java.math.BigDecimal

data class Portfolio(
    val id: Int,
    val userId: Int,
    val brokerId: Int,
    val brokerName: String?,
    val name: String,
    val baseCurrency: String
)

data class Holding(
    val id: Int,
    val portfolioId: Int,
    val securityId: Int,
    val security: Security?,
    val quantity: BigDecimal,
    val averageCost: BigDecimal,
    val totalCost: BigDecimal
)

data class HoldingWithPnL(
    val holding: Holding,
    val currentPrice: BigDecimal,
    val marketValue: BigDecimal,
    val unrealizedPnL: BigDecimal,
    val unrealizedPct: BigDecimal
)

data class PortfolioSummary(
    val portfolioId: Int,
    val totalCost: BigDecimal,
    val totalMarketValue: BigDecimal,
    val totalUnrealizedPnL: BigDecimal,
    val totalUnrealizedPct: BigDecimal,
    val holdings: List<HoldingWithPnL>
)

data class Security(
    val id: Int,
    val symbol: String,
    val name: String,
    val type: SecurityType,
    val currency: String
)

enum class SecurityType(val value: String) {
    STOCK("stock"),
    ETF("etf"),
    BOND("bond"),
    FUND("fund"),
    CRYPTO("crypto"),
    METAL("metal");

    companion object {
        fun fromValue(value: String): SecurityType {
            return entries.find { it.value == value } ?: STOCK
        }
    }
}

data class Trade(
    val id: Int,
    val portfolioId: Int,
    val securityId: Int,
    val security: Security?,
    val tradeDate: String,
    val side: TradeSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val fee: BigDecimal,
    val note: String?
)

enum class TradeSide(val value: String) {
    BUY("buy"),
    SELL("sell");

    companion object {
        fun fromValue(value: String): TradeSide {
            return entries.find { it.value == value } ?: BUY
        }
    }
}

data class NewTrade(
    val portfolioId: Int,
    val securityId: Int,
    val side: TradeSide,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val fee: BigDecimal = BigDecimal.ZERO,
    val tradeDate: String,
    val note: String? = null
)

data class Broker(
    val id: Int,
    val userId: Int,
    val name: String
)
