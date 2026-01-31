package com.barghest.bux.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.TopCategory
import com.barghest.bux.domain.model.TrendItem
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика расходов") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Trends section
                    item {
                        Text(
                            "Тренды по месяцам",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.trends.isEmpty()) {
                        item {
                            Text("Нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(state.trends) { trend ->
                            TrendCard(trend)
                        }
                    }

                    // Top categories section
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Топ категорий расходов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.topCategories.isEmpty()) {
                        item {
                            Text("Нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val maxTotal = state.topCategories.maxOfOrNull { it.total } ?: BigDecimal.ONE
                        items(state.topCategories) { category ->
                            TopCategoryCard(category, maxTotal)
                        }
                    }
                }
            }
        }

        state.error?.let {
            // Show snackbar or error text
        }
    }
}

@Composable
private fun TrendCard(trend: TrendItem) {
    val monthName = Month.of(trend.month).getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
    val changePercent = if (trend.prevExpense != null && trend.prevExpense > BigDecimal.ZERO) {
        trend.expense.subtract(trend.prevExpense)
            .multiply(BigDecimal(100))
            .divide(trend.prevExpense, 1, RoundingMode.HALF_UP)
    } else null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$monthName ${trend.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (changePercent != null) {
                    val isUp = changePercent > BigDecimal.ZERO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isUp) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isUp) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${if (isUp) "+" else ""}${changePercent.toPlainString()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUp) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Доходы", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${trend.income.setScale(0, RoundingMode.HALF_UP).toPlainString()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Расходы", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${trend.expense.setScale(0, RoundingMode.HALF_UP).toPlainString()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Баланс", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${trend.net.setScale(0, RoundingMode.HALF_UP).toPlainString()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCategoryCard(category: TopCategory, maxTotal: BigDecimal) {
    val fraction = if (maxTotal > BigDecimal.ZERO) {
        category.total.divide(maxTotal, 2, RoundingMode.HALF_UP).toFloat()
    } else 0f

    val color = try {
        Color(android.graphics.Color.parseColor(category.categoryColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.categoryIcon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category.categoryName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${category.total.setScale(0, RoundingMode.HALF_UP).toPlainString()} ₽",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${category.count} операций",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
