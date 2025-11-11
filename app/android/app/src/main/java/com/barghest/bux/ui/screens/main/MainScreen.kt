package com.barghest.bux.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = koinViewModel()
) {
    val transactions = viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Мои финансы") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") },
                elevation = FloatingActionButtonDefaults.elevation(1.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить транзакцию")
            }
        }

    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (transactions.value.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Пока нет записей 🪙")
                    }
                }
            } else {
                items(transactions.value.size) { i ->
                    val item = transactions.value[i]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.amount} ₽")
                        }
                    }
                }
            }
        }
    }
}
