package com.barghest.bux.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.data.local.PreferencesManager
import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.domain.model.ThemeMode
import com.barghest.bux.ui.application.navigation.Screen
import org.koin.androidx.compose.get

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val preferencesManager: PreferencesManager = get()
    val authRepository: AuthRepository = get()
    val themeMode by preferencesManager.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Профиль",
                style = MaterialTheme.typography.titleMedium
            )
            ListItem(
                headlineContent = { Text("Редактировать профиль") },
                leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate(Screen.ProfileEdit.route) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Внешний вид",
                style = MaterialTheme.typography.titleMedium
            )

            ThemeOption(
                title = "Системная",
                mode = ThemeMode.SYSTEM,
                selectedMode = themeMode,
                onSelect = preferencesManager::setThemeMode
            )
            ThemeOption(
                title = "Светлая",
                mode = ThemeMode.LIGHT,
                selectedMode = themeMode,
                onSelect = preferencesManager::setThemeMode
            )
            ThemeOption(
                title = "Тёмная",
                mode = ThemeMode.DARK,
                selectedMode = themeMode,
                onSelect = preferencesManager::setThemeMode
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Аккаунт",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = {
                    authRepository.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Выйти")
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
        trailingContent = {
            RadioButton(
                selected = mode == selectedMode,
                onClick = { onSelect(mode) }
            )
        },
        modifier = Modifier.clickable { onSelect(mode) }
    )
}
