package com.barghest.bux.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrokerResponse(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    val name: String
)

@Serializable
data class PortfolioResponse(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("broker_id") val brokerId: Int,
    val broker: BrokerResponse? = null,
    val name: String,
    @SerialName("base_currency") val baseCurrency: String
)

@Serializable
data class SecurityResponse(
    val id: Int,
    val symbol: String,
    val name: String,
    val type: String,
    val currency: String
)

@Serializable
data class HoldingResponse(
    val id: Int,
    @SerialName("portfolio_id") val portfolioId: Int,
    @SerialName("security_id") val securityId: Int,
    val security: SecurityResponse? = null,
    val quantity: String,
    @SerialName("average_cost") val averageCost: String,
    @SerialName("total_cost") val totalCost: String
)

@Serializable
data class HoldingWithPnLResponse(
    val holding: HoldingResponse,
    @SerialName("current_price") val currentPrice: String,
    @SerialName("market_value") val marketValue: String,
    @SerialName("unrealized_pnl") val unrealizedPnL: String,
    @SerialName("unrealized_pct") val unrealizedPct: String
)

@Serializable
data class PortfolioSummaryResponse(
    @SerialName("portfolio_id") val portfolioId: Int,
    @SerialName("total_cost") val totalCost: String,
    @SerialName("total_market_value") val totalMarketValue: String,
    @SerialName("total_unrealized_pnl") val totalUnrealizedPnL: String,
    @SerialName("total_unrealized_pct") val totalUnrealizedPct: String,
    val holdings: List<HoldingWithPnLResponse>
)

@Serializable
data class TradeResponse(
    val id: Int,
    @SerialName("portfolio_id") val portfolioId: Int,
    @SerialName("security_id") val securityId: Int,
    val security: SecurityResponse? = null,
    @SerialName("trade_date") val tradeDate: String,
    val side: String,
    val quantity: String,
    val price: String,
    val fee: String,
    val note: String? = null
)

@Serializable
data class CreateTradeRequest(
    @SerialName("portfolio_id") val portfolioId: Int,
    @SerialName("security_id") val securityId: Int,
    val side: String,
    val quantity: String,
    val price: String,
    val fee: String,
    @SerialName("trade_date") val tradeDate: String,
    val note: String? = null
)

@Serializable
data class CreatePortfolioRequest(
    @SerialName("user_id") val userId: Int,
    @SerialName("broker_id") val brokerId: Int,
    val name: String,
    @SerialName("base_currency") val baseCurrency: String = "RUB"
)

@Serializable
data class CreateBrokerRequest(
    @SerialName("user_id") val userId: Int,
    val name: String
)
