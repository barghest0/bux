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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barghest.bux.data.local.BuxDatabase
import com.barghest.bux.data.local.PreferencesManager
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.domain.service.BiometricHelper
import com.barghest.bux.domain.model.ThemeMode
import com.barghest.bux.ui.application.navigation.Screen
import com.barghest.bux.ui.application.network.isInternetAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.get

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val preferencesManager: PreferencesManager = get()
    val authRepository: AuthRepository = get()
    val biometricHelper: BiometricHelper = get()
    val tokenManager: TokenManager = get()
    val database: BuxDatabase = get()
    val themeMode by preferencesManager.themeMode.collectAsState()
    val biometricEnabled by preferencesManager.biometricEnabled.collectAsState()
    val showResetDialog = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

            if (biometricHelper.canAuthenticate()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Безопасность",
                    style = MaterialTheme.typography.titleMedium
                )
                ListItem(
                    headlineContent = { Text("Вход по биометрии") },
                    leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { preferencesManager.setBiometricEnabled(it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Аккаунт",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = {
                    authRepository.logout()
                    val hasInternet = context.isInternetAvailable()
                    if (!hasInternet) {
                        preferencesManager.setOfflineMode(true)
                    } else {
                        preferencesManager.setOfflineMode(false)
                    }
                    navController.navigate(
                        if (hasInternet) Screen.Login.route else Screen.Home.route
                    ) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showResetDialog.value = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = "Сбросить локальные данные")
            }
        }
    }

    if (showResetDialog.value) {
        AlertDialog(
            onDismissRequest = { showResetDialog.value = false },
            title = { Text("Сброс локальных данных") },
            text = {
                Text("Все локальные данные будут удалены. Продолжить?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog.value = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.clearAllTables()
                            }
                            preferencesManager.clearAll()
                            tokenManager.clearAll()
                            val hasInternet = context.isInternetAvailable()
                            if (!hasInternet) {
                                preferencesManager.setOfflineMode(true)
                            }
                            navController.navigate(
                                if (hasInternet) Screen.Login.route else Screen.Home.route
                            ) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                Button(onClick = { showResetDialog.value = false }) {
                    Text("Отмена")
                }
            }
        )
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
