package com.barghest.bux.ui.application.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.barghest.bux.ui.screens.accounts.AccountsScreen
import com.barghest.bux.ui.screens.accounts.AddAccountScreen
import com.barghest.bux.ui.screens.auth.LoginScreen
import com.barghest.bux.ui.screens.home.HomeScreen
import com.barghest.bux.ui.screens.main.MainScreen
import com.barghest.bux.ui.screens.transaction.add.AddTransactionScreen
import com.barghest.bux.ui.screens.categories.CategoriesScreen
import com.barghest.bux.ui.screens.categories.AddCategoryScreen
import com.barghest.bux.ui.screens.categories.EditCategoryScreen
import com.barghest.bux.ui.screens.investments.AddTradeScreen
import com.barghest.bux.ui.screens.investments.PortfolioDetailScreen
import com.barghest.bux.ui.screens.investments.PortfoliosScreen
import com.barghest.bux.ui.screens.analytics.AnalyticsDashboardScreen
import com.barghest.bux.ui.screens.analytics.NetWorthScreen
import com.barghest.bux.ui.screens.budgets.AddBudgetScreen
import com.barghest.bux.ui.screens.budgets.BudgetsScreen
import com.barghest.bux.ui.screens.settings.ProfileEditScreen
import com.barghest.bux.ui.screens.recurring.RecurringTransactionsScreen
import com.barghest.bux.ui.screens.recurring.AddRecurringTransactionScreen
import com.barghest.bux.ui.screens.insights.InsightsScreen
import com.barghest.bux.ui.screens.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Main : Screen("main")
    data object AddTransaction : Screen("add_transaction")
    data object Login : Screen("login")
    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account")
    data object Categories : Screen("categories")
    data object AddCategory : Screen("add_category")
    data object EditCategory : Screen("edit_category/{categoryId}")
    data object Portfolios : Screen("portfolios")
    data object PortfolioDetail : Screen("portfolio/{portfolioId}")
    data object AddTrade : Screen("add_trade/{portfolioId}")
    data object Analytics : Screen("analytics")
    data object NetWorth : Screen("net_worth")
    data object Budgets : Screen("budgets")
    data object AddBudget : Screen("add_budget")
    data object Settings : Screen("settings")
    data object ProfileEdit : Screen("profile_edit")
    data object RecurringTransactions : Screen("recurring_transactions")
    data object AddRecurringTransaction : Screen("add_recurring_transaction")
    data object Insights : Screen("insights")
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Analytics.route) {
            AnalyticsDashboardScreen(navController)
        }
        composable(Screen.NetWorth.route) {
            NetWorthScreen(navController)
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
        composable(Screen.Categories.route) {
            CategoriesScreen(navController)
        }
        composable(Screen.AddCategory.route) {
            AddCategoryScreen(navController)
        }
        composable(
            route = Screen.EditCategory.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
            EditCategoryScreen(navController, categoryId)
        }
        composable(Screen.Portfolios.route) {
            PortfoliosScreen(navController)
        }
        composable(
            route = Screen.PortfolioDetail.route,
            arguments = listOf(navArgument("portfolioId") { type = NavType.IntType })
        ) {
            PortfolioDetailScreen(navController)
        }
        composable(
            route = Screen.AddTrade.route,
            arguments = listOf(navArgument("portfolioId") { type = NavType.IntType })
        ) {
            AddTradeScreen(navController)
        }
        composable(Screen.Budgets.route) {
            BudgetsScreen(navController)
        }
        composable(Screen.AddBudget.route) {
            AddBudgetScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.ProfileEdit.route) {
            ProfileEditScreen(navController)
        }
        composable(Screen.RecurringTransactions.route) {
            RecurringTransactionsScreen(navController)
        }
        composable(Screen.AddRecurringTransaction.route) {
            AddRecurringTransactionScreen(navController)
        }
        composable(Screen.Insights.route) {
            InsightsScreen(navController)
        }
    }
}
