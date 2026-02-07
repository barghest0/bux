package com.barghest.bux.ui.application.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.barghest.bux.ui.screens.accounts.AccountsScreen
import com.barghest.bux.ui.screens.accounts.AddAccountScreen
import com.barghest.bux.ui.screens.accounts.AccountDetailScreen
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
import com.barghest.bux.domain.model.TransactionType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAB_TRANSITION_DURATION_MS = 320
private const val TAB_FADE_DURATION_MS = 210
private const val TAB_ENTER_INITIAL_ALPHA = 0.25f
private const val TAB_NAVIGATION_LOCK_MS = TAB_TRANSITION_DURATION_MS.toLong() + 40L
val FloatingTabsBottomContentPadding = 96.dp
private const val ROUTE_FADE_DURATION_MS = 220
private const val ROUTE_SHIFT_DURATION_MS = 300
private const val ROUTE_SHIFT_DIVISOR = 10
private const val ROUTE_ENTER_INITIAL_ALPHA = 0.25f

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Main : Screen("main")
    data object AddTransaction : Screen("add_transaction") {
        const val TYPE_ARG = "type"
        val routeWithArg = "$route?$TYPE_ARG={$TYPE_ARG}"

        fun createRoute(type: TransactionType): String = "$route?$TYPE_ARG=${type.value}"
    }
    data object Login : Screen("login")
    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account")
    data object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Int): String = "account_detail/$accountId"
    }
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
fun NavigationGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTabs = currentRoute != null &&
        currentRoute != Screen.Login.route &&
        currentRoute != Screen.Settings.route &&
        currentRoute != Screen.ProfileEdit.route
    val selectedTab = selectedTabForRoute(currentRoute)
    var isTabNavigationLocked by remember { mutableStateOf(false) }

    LaunchedEffect(isTabNavigationLocked) {
        if (isTabNavigationLocked) {
            delay(TAB_NAVIGATION_LOCK_MS)
            isTabNavigationLocked = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = defaultRouteEnterTransition(),
                exitTransition = defaultRouteExitTransition(),
                popEnterTransition = defaultRoutePopEnterTransition(),
                popExitTransition = defaultRoutePopExitTransition()
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(navController)
                }
                composable(
                    route = Screen.Home.route,
                    enterTransition = tabEnterTransition(),
                    exitTransition = tabExitTransition(),
                    popEnterTransition = tabEnterTransition(),
                    popExitTransition = tabExitTransition()
                ) {
                    HomeScreen(navController)
                }
                composable(
                    route = Screen.Analytics.route,
                    enterTransition = tabEnterTransition(),
                    exitTransition = tabExitTransition(),
                    popEnterTransition = tabEnterTransition(),
                    popExitTransition = tabExitTransition()
                ) {
                    AnalyticsDashboardScreen(navController)
                }
                composable(Screen.NetWorth.route) {
                    NetWorthScreen(navController)
                }
                composable(Screen.Main.route) {
                    MainScreen(navController)
                }
                composable(
                    route = Screen.AddTransaction.routeWithArg,
                    arguments = listOf(
                        navArgument(Screen.AddTransaction.TYPE_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val initialType = backStackEntry.arguments?.getString(Screen.AddTransaction.TYPE_ARG)
                    AddTransactionScreen(
                        navController = navController,
                        initialType = initialType
                    )
                }
                composable(
                    route = Screen.Accounts.route,
                    enterTransition = tabEnterTransition(),
                    exitTransition = tabExitTransition(),
                    popEnterTransition = tabEnterTransition(),
                    popExitTransition = tabExitTransition()
                ) {
                    AccountsScreen(navController)
                }
                composable(Screen.AddAccount.route) {
                    AddAccountScreen(navController)
                }
                composable(
                    route = Screen.AccountDetail.route,
                    arguments = listOf(navArgument("accountId") { type = NavType.IntType })
                ) {
                    AccountDetailScreen(navController)
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
                composable(
                    route = Screen.Portfolios.route,
                    enterTransition = tabEnterTransition(),
                    exitTransition = tabExitTransition(),
                    popEnterTransition = tabEnterTransition(),
                    popExitTransition = tabExitTransition()
                ) {
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

            if (showTabs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    FloatingTabsBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (isTabNavigationLocked) return@FloatingTabsBar

                            val normalizedRoute = currentRoute?.substringBefore("?")
                            if (normalizedRoute == tab.route) return@FloatingTabsBar

                            isTabNavigationLocked = true

                            navController.navigate(tab.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun defaultRouteEnterTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(ROUTE_SHIFT_DURATION_MS),
        initialOffsetX = { fullWidth -> fullWidth / ROUTE_SHIFT_DIVISOR }
    ) + fadeIn(
        animationSpec = tween(ROUTE_FADE_DURATION_MS),
        initialAlpha = ROUTE_ENTER_INITIAL_ALPHA
    )
}

private fun defaultRouteExitTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(ROUTE_SHIFT_DURATION_MS),
        targetOffsetX = { fullWidth -> -fullWidth / ROUTE_SHIFT_DIVISOR }
    ) + fadeOut(
        animationSpec = tween(ROUTE_FADE_DURATION_MS),
        targetAlpha = 0f
    )
}

private fun defaultRoutePopEnterTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(ROUTE_SHIFT_DURATION_MS),
        initialOffsetX = { fullWidth -> -fullWidth / ROUTE_SHIFT_DIVISOR }
    ) + fadeIn(
        animationSpec = tween(ROUTE_FADE_DURATION_MS),
        initialAlpha = ROUTE_ENTER_INITIAL_ALPHA
    )
}

private fun defaultRoutePopExitTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(ROUTE_SHIFT_DURATION_MS),
        targetOffsetX = { fullWidth -> fullWidth / ROUTE_SHIFT_DIVISOR }
    ) + fadeOut(
        animationSpec = tween(ROUTE_FADE_DURATION_MS),
        targetAlpha = 0f
    )
}

private enum class FloatingTab(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    HOME(Screen.Home.route, Icons.Default.Home, "Главная"),
    ACCOUNTS(Screen.Accounts.route, Icons.Default.Wallet, "Счета"),
    INVESTMENTS(Screen.Portfolios.route, Icons.AutoMirrored.Filled.ShowChart, "Инвестиции"),
    ANALYTICS(Screen.Analytics.route, Icons.Default.PieChart, "Аналитика"),
}

private val tabRouteOrder = listOf(
    Screen.Home.route,
    Screen.Accounts.route,
    Screen.Portfolios.route,
    Screen.Analytics.route
)

private fun tabIndexForRoute(route: String?): Int? {
    if (route == null) return null
    val normalized = route.substringBefore("?")
    return tabRouteOrder.indexOf(normalized).takeIf { it >= 0 }
}

private fun tabEnterTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = {
    val from = tabIndexForRoute(initialState.destination.route)
    val to = tabIndexForRoute(targetState.destination.route)
    if (from == null || to == null || from == to) {
        EnterTransition.None
    } else if (to > from) {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(TAB_TRANSITION_DURATION_MS)
        ) + fadeIn(
            animationSpec = tween(TAB_FADE_DURATION_MS),
            initialAlpha = TAB_ENTER_INITIAL_ALPHA
        )
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(TAB_TRANSITION_DURATION_MS)
        ) + fadeIn(
            animationSpec = tween(TAB_FADE_DURATION_MS),
            initialAlpha = TAB_ENTER_INITIAL_ALPHA
        )
    }
}

private fun tabExitTransition():
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = {
    val from = tabIndexForRoute(initialState.destination.route)
    val to = tabIndexForRoute(targetState.destination.route)
    if (from == null || to == null || from == to) {
        ExitTransition.None
    } else if (to > from) {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(TAB_TRANSITION_DURATION_MS)
        ) + fadeOut(
            animationSpec = tween(TAB_FADE_DURATION_MS),
            targetAlpha = 0f
        )
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(TAB_TRANSITION_DURATION_MS)
        ) + fadeOut(
            animationSpec = tween(TAB_FADE_DURATION_MS),
            targetAlpha = 0f
        )
    }
}

private fun selectedTabForRoute(route: String?): FloatingTab = when {
    route == Screen.Accounts.route ||
        route == Screen.AddAccount.route ||
        route == Screen.AccountDetail.route -> FloatingTab.ACCOUNTS
    route == Screen.Portfolios.route ||
        route == Screen.PortfolioDetail.route ||
        route == Screen.AddTrade.route -> FloatingTab.INVESTMENTS
    route == Screen.Analytics.route ||
        route == Screen.NetWorth.route ||
        route == Screen.Insights.route -> FloatingTab.ANALYTICS
    else -> FloatingTab.HOME
}

@Composable
private fun FloatingTabsBar(
    selectedTab: FloatingTab,
    onTabSelected: (FloatingTab) -> Unit
) {
    val tabs = FloatingTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.height(66.dp),
            shape = RoundedCornerShape(33.dp),
            tonalElevation = 4.dp,
            shadowElevation = 9.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val segmentWidth = maxWidth / tabs.size
                val tabItemHorizontalPadding = 2.dp
                val tabItemShape = RoundedCornerShape(percent = 50)
                val indicatorWidth = segmentWidth - (tabItemHorizontalPadding * 2)
                val indicatorHeight = maxHeight
                val indicatorTarget = segmentWidth * selectedIndex + (segmentWidth - indicatorWidth) / 2
                val animatedOffset by animateDpAsState(
                    targetValue = indicatorTarget,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 600f),
                    label = "floating_tabs_indicator"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .size(width = indicatorWidth, height = indicatorHeight)
                        .shadow(
                            elevation = 8.dp,
                            shape = tabItemShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            shape = tabItemShape
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = tabItemHorizontalPadding)
                                .clip(tabItemShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tint
                            )
                            Text(
                                text = tab.label,
                                color = tint,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
