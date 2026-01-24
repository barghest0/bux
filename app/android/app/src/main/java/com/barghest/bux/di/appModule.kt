package com.barghest.bux.di

import com.barghest.bux.data.local.BuxDatabase
import com.barghest.bux.data.local.TokenManager
import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.AccountRepository
import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.service.AuthService
import com.barghest.bux.domain.service.TransactionService
import com.barghest.bux.ui.screens.accounts.AccountsViewModel
import com.barghest.bux.ui.screens.accounts.AddAccountViewModel
import com.barghest.bux.ui.screens.auth.LoginViewModel
import com.barghest.bux.ui.screens.main.MainViewModel
import com.barghest.bux.ui.screens.transaction.add.AddTransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local storage
    single { TokenManager(androidContext()) }
    single { BuxDatabase.getDatabase(androidContext()) }
    single { get<BuxDatabase>().accountDao() }
    single { get<BuxDatabase>().transactionDao() }

    // Network
    single { Api(get()) }

    // User ID provider (placeholder - should come from auth state)
    single<() -> Int> { { 1 } }

    // Repositories
    single { AccountRepository(get(), get(), get()) }
    single { TransactionRepository(get(), get(), get()) }
    single { AuthRepository(get(), get()) }

    // Services
    single { TransactionService(get()) }
    single { AuthService(get()) }

    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { AddTransactionViewModel(get(), get()) }
    viewModel { AccountsViewModel(get()) }
    viewModel { AddAccountViewModel(get()) }
}
