package com.barghest.bux.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.Account
import com.barghest.bux.domain.model.Category
import com.barghest.bux.domain.model.RecurrenceFrequency
import com.barghest.bux.domain.model.TransactionType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionScreen(
    navController: NavController,
    viewModel: AddRecurringTransactionViewModel = koinViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая регулярная операция") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type selector
            Text("Тип операции", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.updateType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size)
                    ) {
                        Text(when(type) {
                            TransactionType.INCOME -> "Доход"
                            TransactionType.EXPENSE -> "Расход"
                            TransactionType.TRANSFER -> "Перевод"
                        })
                    }
                }
            }

            // Account dropdown
            AccountDropdown(
                accounts = accounts,
                selected = state.selectedAccount,
                onSelect = viewModel::updateAccount
            )

            // Category dropdown
            CategoryDropdown(
                categories = categories,
                selected = state.selectedCategory,
                onSelect = viewModel::updateCategory
            )

            // Amount
            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::updateAmount,
                label = { Text("Сумма") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Frequency selector
            Text("Частота", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val frequencies = RecurrenceFrequency.entries
                frequencies.forEachIndexed { index, freq ->
                    SegmentedButton(
                        selected = state.frequency == freq,
                        onClick = { viewModel.updateFrequency(freq) },
                        shape = SegmentedButtonDefaults.itemShape(index, frequencies.size)
                    ) {
                        Text(when(freq) {
                            RecurrenceFrequency.DAILY -> "День"
                            RecurrenceFrequency.WEEKLY -> "Нед"
                            RecurrenceFrequency.MONTHLY -> "Мес"
                            RecurrenceFrequency.YEARLY -> "Год"
                        })
                    }
                }
            }

            // Next date
            OutlinedTextField(
                value = state.nextDateText,
                onValueChange = viewModel::updateNextDate,
                label = { Text("Дата начала (ГГГГ-ММ-ДД)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Error
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // Save button
            Button(
                onClick = { viewModel.save { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    accounts: List<Account>,
    selected: Account?,
    onSelect: (Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Выберите счёт",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onSelect(account)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.icon} ${it.name}" } ?: "Без категории",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Без категории") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
