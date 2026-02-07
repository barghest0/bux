package com.barghest.bux.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionSummary
import com.barghest.bux.domain.model.TransactionType
import com.barghest.bux.ui.screens.accounts.formatMoney
import com.barghest.bux.ui.screens.accounts.icon
import com.barghest.bux.ui.shared.theme.LocalBuxColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") },
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить операцию")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is HomeState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HomeState.Error -> {
                    ErrorContent(
                        message = s.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HomeState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HomeContent(
                            state = s,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ошибка загрузки",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
private fun HomeContent(state: HomeState.Success, navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Счета", "Портфели")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top bar: settings + notifications
        item {
            MinimalTopBar(
                onSettingsClick = { navController.navigate("settings") },
                onAnalyticsClick = { navController.navigate("analytics") }
            )
        }

        // Net worth hero
        item {
            NetWorthHeader(
                netWorth = state.netWorth,
                change = state.netWorthChange,
                onClick = { navController.navigate("net_worth") }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Tab row
        item {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }
        }

        // Pager content (rendered inline in LazyColumn)
        item {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillParentMaxWidth()
            ) { page ->
                when (page) {
                    0 -> AccountsPage(
                        accounts = state.accounts,
                        summary = state.summary,
                        recentTransactions = state.recentTransactions,
                        insight = state.insight,
                        navController = navController
                    )
                    1 -> PortfoliosPage(
                        portfolios = state.portfolios,
                        netWorth = state.netWorth,
                        insight = state.insight,
                        navController = navController
                    )
                }
            }
        }
    }
}

// ─── Top Bar ───

@Composable
private fun MinimalTopBar(onSettingsClick: () -> Unit, onAnalyticsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Настройки",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAnalyticsClick) {
            Icon(
                Icons.Default.PieChart,
                contentDescription = "Аналитика",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Net Worth Header ───

@Composable
private fun NetWorthHeader(
    netWorth: NetWorthData,
    change: BigDecimal?,
    onClick: () -> Unit
) {
    val buxColors = LocalBuxColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Общий капитал",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Main amount — show primary currency large
        val primaryEntry = netWorth.totalByCurrency.entries.maxByOrNull { it.value }
        if (primaryEntry != null) {
            Text(
                text = formatMoney(primaryEntry.value.toDouble(), primaryEntry.key),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Secondary currencies
        netWorth.totalByCurrency.entries
            .filter { it.key != (primaryEntry?.key ?: "") }
            .forEach { (currency, total) ->
                Text(
                    text = formatMoney(total.toDouble(), currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        // Change indicator
        if (change != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val isPositive = change >= BigDecimal.ZERO
                val color = if (isPositive) buxColors.positive else buxColors.negative
                val icon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
                else Icons.AutoMirrored.Filled.TrendingDown

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${change.toPlainString()}% за месяц",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

// ─── Accounts Page ───

@Composable
private fun AccountsPage(
    accounts: List<Account>,
    summary: TransactionSummary?,
    recentTransactions: List<Transaction>,
    insight: InsightMessage?,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Accounts grouped list
        if (accounts.isEmpty()) {
            EmptyAccountsCard(
                onClick = { navController.navigate("add_account") }
            )
        } else {
            AccountsGroupedList(
                accounts = accounts,
                onSeeAll = { navController.navigate("accounts") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Pulse
        if (summary != null) {
            MonthlyPulseCard(
                summary = summary,
                onClick = { navController.navigate("analytics") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Insight nudge
        if (insight != null) {
            InsightNudge(insight = insight)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent transactions
        if (recentTransactions.isNotEmpty()) {
            RecentTransactionsSection(
                transactions = recentTransactions,
                onSeeAll = { navController.navigate("main") }
            )
        }
    }
}

@Composable
private fun EmptyAccountsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Добавьте первый счет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountsGroupedList(accounts: List<Account>, onSeeAll: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои счета",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSeeAll) {
                Text("Все", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                accounts.forEachIndexed { index, account ->
                    AccountRow(account = account)
                    if (index < accounts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            color = LocalBuxColors.current.divider.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon in circle
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = account.type.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = account.type.displayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatMoney(account.balance.toDouble(), account.currency),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun AccountType.displayLabel(): String = when (this) {
    AccountType.BANK_ACCOUNT -> "Банковский счет"
    AccountType.CARD -> "Карта"
    AccountType.CASH -> "Наличные"
    AccountType.CRYPTO -> "Крипто"
    AccountType.INVESTMENT -> "Инвестиции"
    AccountType.PROPERTY -> "Недвижимость"
}

// ─── Monthly Pulse ───

@Composable
private fun MonthlyPulseCard(summary: TransactionSummary, onClick: () -> Unit) {
    val buxColors = LocalBuxColors.current
    val maxAmount = maxOf(summary.totalIncome, summary.totalExpense)
    val incomeRatio = if (maxAmount > BigDecimal.ZERO)
        summary.totalIncome.toFloat() / maxAmount.toFloat() else 0f
    val expenseRatio = if (maxAmount > BigDecimal.ZERO)
        summary.totalExpense.toFloat() / maxAmount.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Этот период",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Income bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Доходы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
                )
                LinearProgressIndicator(
                    progress = { incomeRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = buxColors.positive.copy(alpha = 0.8f),
                    trackColor = buxColors.positive.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatMoney(summary.totalIncome.toDouble(), "RUB"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = buxColors.positive
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expense bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Расходы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
                )
                LinearProgressIndicator(
                    progress = { expenseRatio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = buxColors.negative.copy(alpha = 0.6f),
                    trackColor = buxColors.negative.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatMoney(summary.totalExpense.toDouble(), "RUB"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = buxColors.negative
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = LocalBuxColors.current.divider.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Net result
            val isPositive = summary.net >= BigDecimal.ZERO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Чистый",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${formatMoney(summary.net.toDouble(), "RUB")}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) buxColors.positive else buxColors.negative
                )
            }
        }
    }
}

// ─── Insight Nudge ───

@Composable
private fun InsightNudge(insight: InsightMessage) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = insight.icon,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = insight.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { visible = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Скрыть",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ─── Recent Transactions ───

@Composable
private fun RecentTransactionsSection(transactions: List<Transaction>, onSeeAll: () -> Unit) {
    val buxColors = LocalBuxColors.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
        .withZone(ZoneId.systemDefault())

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Последние операции",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSeeAll) {
                Text("Все", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        transactions.forEachIndexed { index, tx ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description ?: tx.type.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateFormatter.format(tx.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val isIncome = tx.type == TransactionType.INCOME
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatMoney(tx.amount.toDouble(), tx.currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) buxColors.positive else MaterialTheme.colorScheme.onSurface
                )
            }
            if (index < transactions.lastIndex) {
                HorizontalDivider(
                    color = LocalBuxColors.current.divider.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

private fun TransactionType.label(): String = when (this) {
    TransactionType.INCOME -> "Доход"
    TransactionType.EXPENSE -> "Расход"
    TransactionType.TRANSFER -> "Перевод"
}

// ─── Portfolios Page ───

@Composable
private fun PortfoliosPage(
    portfolios: List<PortfolioWithSummary>,
    netWorth: NetWorthData,
    insight: InsightMessage?,
    navController: NavController
) {
    val buxColors = LocalBuxColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        if (portfolios.isEmpty()) {
            EmptyPortfoliosCard(onClick = { navController.navigate("portfolios") })
        } else {
            // Total investment value
            val totalMarketValue = portfolios.mapNotNull { it.summary?.totalMarketValue }
                .fold(BigDecimal.ZERO) { sum, v -> sum.add(v) }
            val totalPnL = portfolios.mapNotNull { it.summary?.totalUnrealizedPnL }
                .fold(BigDecimal.ZERO) { sum, v -> sum.add(v) }
            val totalCost = portfolios.mapNotNull { it.summary?.totalCost }
                .fold(BigDecimal.ZERO) { sum, v -> sum.add(v) }
            val totalPct = if (totalCost > BigDecimal.ZERO)
                totalPnL.multiply(BigDecimal(100)).divide(totalCost, 1, java.math.RoundingMode.HALF_UP)
            else BigDecimal.ZERO

            // Performance summary header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Портфели",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val currency = portfolios.first().portfolio.baseCurrency
                    Text(
                        text = formatMoney(totalMarketValue.toDouble(), currency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val isPositive = totalPnL >= BigDecimal.ZERO
                    val color = if (isPositive) buxColors.positive else buxColors.negative
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${formatMoney(totalPnL.toDouble(), currency)} (${totalPct.toPlainString()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portfolio list
            PortfolioGroupedList(
                portfolios = portfolios,
                onSeeAll = { navController.navigate("portfolios") },
                onPortfolioClick = { id -> navController.navigate("portfolio/$id") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Allocation donut placeholder
            AllocationCard(portfolios = portfolios)
        }

        // Investment value from net worth
        if (netWorth.investmentValue > BigDecimal.ZERO && portfolios.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            PayoutsSummaryCard()
        }

        // Insight
        if (insight != null) {
            Spacer(modifier = Modifier.height(16.dp))
            InsightNudge(insight = insight)
        }
    }
}

@Composable
private fun EmptyPortfoliosCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Добавьте первый портфель",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PortfolioGroupedList(
    portfolios: List<PortfolioWithSummary>,
    onSeeAll: () -> Unit,
    onPortfolioClick: (Int) -> Unit
) {
    val buxColors = LocalBuxColors.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои портфели",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSeeAll) {
                Text("Все", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                portfolios.forEachIndexed { index, pw ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPortfolioClick(pw.portfolio.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PieChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pw.portfolio.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pw.portfolio.brokerName ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val value = pw.summary?.totalMarketValue ?: BigDecimal.ZERO
                            Text(
                                text = formatMoney(value.toDouble(), pw.portfolio.baseCurrency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val pct = pw.summary?.totalUnrealizedPct ?: BigDecimal.ZERO
                            val isPos = pct >= BigDecimal.ZERO
                            Text(
                                text = "${if (isPos) "+" else ""}${pct.toPlainString()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPos) buxColors.positive else buxColors.negative
                            )
                        }
                    }
                    if (index < portfolios.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp),
                            color = buxColors.divider.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ─── Allocation Card ───

@Composable
private fun AllocationCard(portfolios: List<PortfolioWithSummary>) {
    // Aggregate holdings by security type
    val holdingsByType = mutableMapOf<String, BigDecimal>()
    portfolios.forEach { pw ->
        pw.summary?.holdings?.forEach { h ->
            val type = h.holding.security?.type?.name ?: "OTHER"
            holdingsByType[type] = (holdingsByType[type] ?: BigDecimal.ZERO).add(h.marketValue)
        }
    }

    if (holdingsByType.isEmpty()) return

    val total = holdingsByType.values.fold(BigDecimal.ZERO) { sum, v -> sum.add(v) }
    val sorted = holdingsByType.entries.sortedByDescending { it.value }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        LocalBuxColors.current.positive,
        LocalBuxColors.current.warning,
        MaterialTheme.colorScheme.error
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Аллокация",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Stacked bar as a simple allocation view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                sorted.forEachIndexed { index, (_, value) ->
                    val fraction = if (total > BigDecimal.ZERO)
                        value.toFloat() / total.toFloat() else 0f
                    Box(
                        modifier = Modifier
                            .weight(fraction.coerceAtLeast(0.01f))
                            .height(8.dp)
                            .background(colors[index % colors.size])
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            sorted.forEachIndexed { index, (type, value) ->
                val pct = if (total > BigDecimal.ZERO)
                    value.multiply(BigDecimal(100)).divide(total, 0, java.math.RoundingMode.HALF_UP)
                else BigDecimal.ZERO

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = securityTypeLabel(type),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${pct.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun securityTypeLabel(type: String): String = when (type) {
    "STOCK" -> "Акции"
    "ETF" -> "ETF"
    "BOND" -> "Облигации"
    "FUND" -> "Фонды"
    "CRYPTO" -> "Крипто"
    "METAL" -> "Металлы"
    else -> type
}

// ─── Payouts Summary ───

@Composable
private fun PayoutsSummaryCard() {
    val buxColors = LocalBuxColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Выплаты и комиссии",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Данные о дивидендах и комиссиях появятся после добавления сделок",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
