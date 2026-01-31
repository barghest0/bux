package com.barghest.bux.ui.application

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.barghest.bux.data.local.PreferencesManager
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.domain.model.ThemeMode
import com.barghest.bux.domain.service.BiometricHelper
import com.barghest.bux.ui.application.navigation.NavigationGraph
import com.barghest.bux.ui.shared.theme.BuxTheme
import org.koin.androidx.compose.get

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferencesManager: PreferencesManager = get()
            val tokenManager: TokenManager = get()
            val biometricHelper: BiometricHelper = get()
            val themeMode by preferencesManager.themeMode.collectAsState()
            val biometricEnabled by preferencesManager.biometricEnabled.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var biometricPassed by remember { mutableStateOf(false) }
            val needsBiometric = biometricEnabled && tokenManager.getToken() != null && biometricHelper.canAuthenticate()

            LaunchedEffect(needsBiometric) {
                if (needsBiometric && !biometricPassed) {
                    biometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { biometricPassed = true },
                        onError = { /* stay locked */ }
                    )
                }
            }

            BuxTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (needsBiometric && !biometricPassed) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Приложение заблокировано", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    biometricHelper.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = { biometricPassed = true },
                                        onError = { }
                                    )
                                }) {
                                    Text("Разблокировать")
                                }
                            }
                        }
                    } else {
                        val navController = rememberNavController()
                        NavigationGraph(navController)
                    }
                }
            }
        }
    }
}
