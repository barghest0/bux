package com.barghest.bux.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.AccountType
import com.barghest.bux.domain.model.MonthlySummary
import com.barghest.bux.domain.model.NetWorthData
import com.barghest.bux.domain.model.Transaction
import com.barghest.bux.domain.model.TransactionSummary
import com.barghest.bux.domain.model.TransactionType
import com.barghest.bux.ui.application.navigation.FloatingTabsBottomContentPadding
import com.barghest.bux.ui.application.navigation.Screen
import com.barghest.bux.ui.screens.accounts.formatMoney
import com.barghest.bux.ui.screens.accounts.icon
import com.barghest.bux.ui.shared.theme.LocalBuxColors
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bux") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Профиль"
                        )
                    }
                }
            )
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
    val monthSeries = state.summary?.byMonth
        ?.sortedWith(compareBy<MonthlySummary> { it.year }.thenBy { it.month })
        .orEmpty()
    val fallbackSeries = fallbackMonthSeries()
    val chartSeries = if (monthSeries.isNotEmpty()) monthSeries else fallbackSeries
    val currentMonthSummary = monthSeries.lastOrNull() ?: fallbackSeries.last()
    val primaryCurrency = state.netWorth.totalByCurrency
        .maxByOrNull { it.value }
        ?.key
        ?: "RUB"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 24.dp + FloatingTabsBottomContentPadding
        )
    ) {
        // Net worth hero
        item {
            NetWorthHeader(
                netWorth = state.netWorth,
                change = state.netWorthChange,
                onClick = { navController.navigate("net_worth") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            QuickActionIconsRow(
                onAddExpense = {
                    navController.navigate(
                        Screen.AddTransaction.createRoute(TransactionType.EXPENSE)
                    )
                },
                onAddIncome = {
                    navController.navigate(
                        Screen.AddTransaction.createRoute(TransactionType.INCOME)
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            CurrentMonthIncomeExpenseCard(
                month = currentMonthSummary,
                currency = primaryCurrency
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            NetWorthByMonthsCard(
                months = chartSeries,
                currency = primaryCurrency
            )
        }
    }
}

private fun fallbackMonthSeries(): List<MonthlySummary> {
    val now = YearMonth.now()
    val mockNets = listOf(12000, 18000, 11000, 24000, 9000, 21000)
    return (5 downTo 0).map { monthsBack ->
        val ym = now.minusMonths(monthsBack.toLong())
        val net = BigDecimal(mockNets[5 - monthsBack])
        val expense = BigDecimal(70000 + (monthsBack * 4000))
        val income = expense.add(net)
        MonthlySummary(
            year = ym.year,
            month = ym.monthValue,
            income = income,
            expense = expense,
            net = net
        )
    }
}

@Composable
private fun QuickActionIconsRow(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit
) {
    val buttonBackground = MaterialTheme.colorScheme.primaryContainer
    val iconTint = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        IconButton(
            onClick = onAddExpense,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonBackground)
        ) {
            Icon(
                Icons.Default.SouthWest,
                contentDescription = "Добавить расход",
                tint = iconTint
            )
        }

        IconButton(
            onClick = onAddIncome,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonBackground)
        ) {
            Icon(
                Icons.Default.NorthEast,
                contentDescription = "Добавить доход",
                tint = iconTint
            )
        }
    }
}

@Composable
private fun CurrentMonthIncomeExpenseCard(
    month: MonthlySummary,
    currency: String
) {
    val maxValue = maxOf(month.income, month.expense, BigDecimal.ONE)
    val monthName = Month.of(month.month)
        .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
        .replaceFirstChar { c -> c.uppercase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Доходы и расходы за $monthName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IncomeExpenseBarItem(
                    label = "Доходы",
                    amount = month.income,
                    maxValue = maxValue,
                    currency = currency,
                    color = LocalBuxColors.current.positive,
                    modifier = Modifier.weight(1f)
                )
                IncomeExpenseBarItem(
                    label = "Расходы",
                    amount = month.expense,
                    maxValue = maxValue,
                    currency = currency,
                    color = LocalBuxColors.current.negative,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseBarItem(
    label: String,
    amount: BigDecimal,
    maxValue: BigDecimal,
    currency: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (maxValue > BigDecimal.ZERO) {
        amount.divide(maxValue, 4, java.math.RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatMoney(amount.toDouble(), currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(112.dp)
                .fillMaxWidth(0.45f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(ratio.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.85f))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class NetWorthMonthPoint(
    val label: String,
    val value: Float
)

@Composable
private fun NetWorthByMonthsCard(
    months: List<MonthlySummary>,
    currency: String
) {
    val points = remember(months) {
        val sorted = months.sortedWith(compareBy<MonthlySummary> { it.year }.thenBy { it.month }).takeLast(6)
        var running = BigDecimal.ZERO
        sorted.map { month ->
            running = running.add(month.net)
            NetWorthMonthPoint(
                label = Month.of(month.month)
                    .getDisplayName(TextStyle.SHORT, Locale("ru"))
                    .replaceFirstChar { c -> c.uppercase() },
                value = running.toFloat()
            )
        }
    }
    if (points.isEmpty()) return

    val minValue = points.minOf { it.value }
    val maxValue = points.maxOf { it.value }
    val range = (maxValue - minValue).let { if (it == 0f) 1f else it }
    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selectedPoint = points[selectedIndex]
    val selectedColor = if (selectedPoint.value >= 0f) {
        LocalBuxColors.current.positive
    } else {
        LocalBuxColors.current.negative
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Нетворс по месяцам",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedPoint.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(selectedPoint.value.toDouble(), currency),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = selectedColor
            )
            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val chartHeight = maxHeight
                val zeroY = chartHeight * ((maxValue / range).coerceIn(0f, 1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = zeroY)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEachIndexed { index, point ->
                        val barHeight = chartHeight * (abs(point.value) / range)
                        val topOffset = if (point.value >= 0f) {
                            (zeroY - barHeight).coerceAtLeast(0.dp)
                        } else {
                            zeroY.coerceAtMost(chartHeight - 2.dp)
                        }
                        val barColor = if (point.value >= 0f) {
                            LocalBuxColors.current.positive
                        } else {
                            LocalBuxColors.current.negative
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedIndex = index }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = topOffset)
                                    .width(20.dp)
                                    .height(barHeight.coerceAtLeast(2.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        barColor.copy(alpha = if (index == selectedIndex) 0.95f else 0.45f)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
        Text(
            text = if (primaryEntry != null) {
                formatMoney(primaryEntry.value.toDouble(), primaryEntry.key)
            } else {
                formatMoney(0.0, "RUB")
            },
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

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
    recentTransactions: List<Transaction>,
    insight: InsightMessage?,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = { navController.navigate(Screen.Accounts.route) }) {
                Text("Все", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Счета",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (accounts.isEmpty()) {
            EmptyAccountsAddCard(onClick = { navController.navigate("add_account") })
        } else {
            AccountsCardsRow(
                accounts = accounts,
                onAccountClick = { accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(accountId))
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
private fun EmptyAccountsAddCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(124.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить счет",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountsCardsRow(
    accounts: List<Account>,
    onAccountClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(accounts, key = { it.id }) { account ->
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .height(124.dp)
                    .clickable { onAccountClick(account.id) },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = account.type.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = formatMoney(account.balance.toDouble(), account.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = account.type.displayLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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

// ─── Insight Nudge ───

@Composable
private fun InsightNudge(insight: InsightMessage) {
    var visible by remember { mutableStateOf(true) }
    val shiftPx = with(LocalDensity.current) { 12.dp.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { shiftPx },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { shiftPx }
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Портфелей пока нет",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
