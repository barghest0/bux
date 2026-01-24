package com.barghest.bux.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.domain.model.CategoryType
import org.koin.androidx.compose.koinViewModel

private val availableIcons = listOf(
    "🍔", "🛒", "🏠", "🚗", "💊", "🎬", "✈️", "📱", "👕", "🎁",
    "💰", "💳", "📈", "🏦", "💵", "🎯", "📚", "🏋️", "🎮", "☕"
)

private val availableColors = listOf(
    "#4CAF50", "#2196F3", "#F44336", "#FF9800", "#9C27B0",
    "#00BCD4", "#795548", "#607D8B", "#E91E63", "#3F51B5"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCategoryScreen(
    navController: NavController,
    viewModel: AddCategoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val name by viewModel.name.collectAsState()
    val type by viewModel.type.collectAsState()
    val icon by viewModel.icon.collectAsState()
    val color by viewModel.color.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (state) {
            is AddCategoryState.Success -> {
                navController.popBackStack()
            }
            is AddCategoryState.Error -> {
                snackbarHostState.showSnackbar((state as AddCategoryState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая категория") },
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
            // Type selection
            Text(
                text = "Тип категории",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == CategoryType.EXPENSE,
                    onClick = { viewModel.setType(CategoryType.EXPENSE) },
                    label = { Text("Расход") }
                )
                FilterChip(
                    selected = type == CategoryType.INCOME,
                    onClick = { viewModel.setType(CategoryType.INCOME) },
                    label = { Text("Доход") }
                )
            }

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Icon selection
            Text(
                text = "Иконка",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableIcons.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (icon == emoji) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (icon == emoji) 2.dp else 0.dp,
                                color = if (icon == emoji) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setIcon(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Color selection
            Text(
                text = "Цвет",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableColors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(parseColor(colorHex))
                            .border(
                                width = if (color == colorHex) 3.dp else 0.dp,
                                color = if (color == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setColor(colorHex) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview
            if (name.isNotBlank() && icon.isNotBlank()) {
                Text(
                    text = "Предпросмотр",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parseColor(color)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create button
            Button(
                onClick = { viewModel.createCategory() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is AddCategoryState.Loading
            ) {
                if (state is AddCategoryState.Loading) {
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

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
