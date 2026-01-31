package com.barghest.bux.di

import com.barghest.bux.data.local.BuxDatabase
import com.barghest.bux.data.local.PreferencesManager
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.data.repository.CategoryRepository
import com.barghest.bux.data.repository.AnalyticsRepository
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.data.repository.BudgetRepository
import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.data.sync.SyncManager
import com.barghest.bux.domain.service.AuthService
import com.barghest.bux.domain.service.TransactionService
import com.barghest.bux.ui.screens.accounts.AccountsViewModel
import com.barghest.bux.ui.screens.accounts.AddAccountViewModel
import com.barghest.bux.ui.screens.auth.LoginViewModel
import com.barghest.bux.ui.screens.categories.AddCategoryViewModel
import com.barghest.bux.ui.screens.categories.CategoriesViewModel
import com.barghest.bux.ui.screens.investments.AddTradeViewModel
import com.barghest.bux.ui.screens.investments.PortfolioDetailViewModel
import com.barghest.bux.ui.screens.investments.PortfoliosViewModel
import com.barghest.bux.ui.screens.analytics.AnalyticsDashboardViewModel
import com.barghest.bux.ui.screens.analytics.NetWorthViewModel
import com.barghest.bux.ui.screens.budgets.AddBudgetViewModel
import com.barghest.bux.ui.screens.budgets.BudgetsViewModel
import com.barghest.bux.ui.screens.main.MainViewModel
import com.barghest.bux.ui.screens.settings.ProfileEditViewModel
import com.barghest.bux.ui.screens.transaction.add.AddTransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local storage
    single { TokenManager(androidContext()) }
    single { PreferencesManager(androidContext()) }
    single { BuxDatabase.getDatabase(androidContext()) }
    single { get<BuxDatabase>().accountDao() }
    single { get<BuxDatabase>().transactionDao() }
    single { get<BuxDatabase>().categoryDao() }

    // Network
    single { Api(get()) }

    // User ID provider (placeholder - should come from auth state)
    single<() -> Int> { { 1 } }

    // Repositories
    single { AccountRepository(get(), get(), get()) }
    single { TransactionRepository(get(), get(), get()) }
    single { CategoryRepository(get(), get(), get()) }
    single { AuthRepository(get(), get()) }
    single { InvestmentRepository(get()) }
    single { AnalyticsRepository(get(), get(), get()) }
    single { BudgetRepository(get()) }

    // Sync
    single { SyncManager(get(), get(), get()) }

    // Services
    single { TransactionService(get()) }
    single { AuthService(get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { AddTransactionViewModel(get(), get(), get()) }
    viewModel { AccountsViewModel(get()) }
    viewModel { AddAccountViewModel(get()) }
    viewModel { CategoriesViewModel(get()) }
    viewModel { AddCategoryViewModel(get()) }
    viewModel { PortfoliosViewModel(get()) }
    viewModel { PortfolioDetailViewModel(get(), get()) }
    viewModel { AddTradeViewModel(get(), get()) }
    viewModel { AnalyticsDashboardViewModel(get(), get(), get()) }
    viewModel { NetWorthViewModel(get()) }
    viewModel { BudgetsViewModel(get()) }
    viewModel { AddBudgetViewModel(get(), get()) }
    viewModel { ProfileEditViewModel(get()) }
}
