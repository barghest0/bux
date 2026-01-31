package com.barghest.bux.ui.screens.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.BudgetPeriod
import com.barghest.bux.domain.model.Category
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    navController: NavController,
    viewModel: AddBudgetViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val period by viewModel.period.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (state) {
            is AddBudgetState.Success -> navController.popBackStack()
            is AddBudgetState.Error -> {
                snackbarHostState.showSnackbar((state as AddBudgetState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый бюджет") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category dropdown
            Text(text = "Категория", style = MaterialTheme.typography.titleMedium)
            CategoryDropdown(
                categories = categories,
                selected = selectedCategory,
                onSelect = { viewModel.setCategory(it) }
            )

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { viewModel.setAmount(it) },
                label = { Text("Сумма") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Period
            Text(text = "Период", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = period == BudgetPeriod.MONTHLY,
                    onClick = { viewModel.setPeriod(BudgetPeriod.MONTHLY) },
                    label = { Text("Ежемесячно") }
                )
                FilterChip(
                    selected = period == BudgetPeriod.YEARLY,
                    onClick = { viewModel.setPeriod(BudgetPeriod.YEARLY) },
                    label = { Text("Ежегодно") }
                )
            }

            // Currency
            Text(text = "Валюта", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RUB", "USD", "EUR").forEach { cur ->
                    FilterChip(
                        selected = currency == cur,
                        onClick = { viewModel.setCurrency(cur) },
                        label = { Text(cur) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.createBudget() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is AddBudgetState.Loading
            ) {
                if (state is AddBudgetState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Создать")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.icon} ${it.name}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Выберите категорию") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
            if (categories.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Нет категорий расходов") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
        }
    }
}
