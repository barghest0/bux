package com.barghest.bux.ui.application.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.barghest.bux.ui.screens.main.MainScreen
import com.barghest.bux.ui.screens.transaction.add.AddTransactionScreen

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object AddTransaction : Screen("add_transaction")
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController)
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(navController)
        }
    }
}