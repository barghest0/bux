package com.barghest.bux.ui.application.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.barghest.bux.ui.screens.accounts.AccountsScreen
import com.barghest.bux.ui.screens.accounts.AddAccountScreen
import com.barghest.bux.ui.screens.auth.LoginScreen
import com.barghest.bux.ui.screens.main.MainScreen
import com.barghest.bux.ui.screens.transaction.add.AddTransactionScreen

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object AddTransaction : Screen("add_transaction")
    data object Login : Screen("login")
    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account")
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Main.route) {
            MainScreen(navController)
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(navController)
        }
        composable(Screen.Accounts.route) {
            AccountsScreen(navController)
        }
        composable(Screen.AddAccount.route) {
            AddAccountScreen(navController)
        }
    }
}
