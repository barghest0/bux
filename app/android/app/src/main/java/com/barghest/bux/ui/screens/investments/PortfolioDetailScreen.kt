package com.barghest.bux.ui.screens.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.HoldingWithPnL
import com.barghest.bux.domain.model.PortfolioSummary
import com.barghest.bux.domain.model.Trade
import com.barghest.bux.domain.model.TradeSide
import com.barghest.bux.ui.screens.accounts.formatMoney
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioDetailScreen(
    navController: NavController,
    viewModel: PortfolioDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val s = state) {
                            is PortfolioDetailState.Success -> s.portfolio.name
                            else -> "Портфель"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state is PortfolioDetailState.Success) {
                FloatingActionButton(
                    onClick = {
                        val portfolioId = (state as PortfolioDetailState.Success).portfolio.id
                        navController.navigate("add_trade/$portfolioId")
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить сделку")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = state) {
                is PortfolioDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PortfolioDetailState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Повторить")
                        }
                    }
                }
                is PortfolioDetailState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PortfolioDetailContent(
                            summary = currentState.summary,
                            trades = currentState.trades,
                            currency = currentState.portfolio.baseCurrency
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioDetailContent(
    summary: PortfolioSummary?,
    trades: List<Trade>,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary card
        summary?.let { s ->
            item {
                PortfolioSummaryCard(summary = s, currency = currency)
            }
        }

        // Holdings section
        if (summary != null && summary.holdings.isNotEmpty()) {
            item {
                Text(
                    text = "Позиции",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(summary.holdings) { holding ->
                HoldingCard(holding = holding, currency = currency)
            }
        }

        // Trades section
        item {
            Text(
                text = "История сделок",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (trades.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Нет сделок",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(trades) { trade ->
                TradeCard(trade = trade)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Стоимость портфеля",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(summary.totalMarketValue.toDouble(), currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Вложено",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(summary.totalCost.toDouble(), currency),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Прибыль/Убыток",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isProfit = summary.totalUnrealizedPnL >= BigDecimal.ZERO
                        Icon(
                            imageVector = if (isProfit) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatMoney(summary.totalUnrealizedPnL.toDouble(), currency)} (${summary.totalUnrealizedPct.setScale(2)}%)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingCard(
    holding: HoldingWithPnL,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = holding.holding.security?.symbol ?: "N/A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = holding.holding.security?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(holding.marketValue.toDouble(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val isProfit = holding.unrealizedPnL >= BigDecimal.ZERO
                    Text(
                        text = "${if (isProfit) "+" else ""}${formatMoney(holding.unrealizedPnL.toDouble(), currency)} (${holding.unrealizedPct.setScale(2)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${holding.holding.quantity} шт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ср. цена: ${formatMoney(holding.holding.averageCost.toDouble(), currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Текущая: ${formatMoney(holding.currentPrice.toDouble(), currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TradeCard(trade: Trade) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (trade.side == TradeSide.BUY) "Покупка" else "Продажа",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (trade.side == TradeSide.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trade.security?.symbol ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = trade.tradeDate.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${trade.quantity} x ${trade.price}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val total = trade.quantity.multiply(trade.price).add(trade.fee)
                Text(
                    text = total.toPlainString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
