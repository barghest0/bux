package com.barghest.bux.di

import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.AuthRepository
import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.service.AuthService
import com.barghest.bux.domain.service.TransactionService
import com.barghest.bux.ui.screens.auth.LoginViewModel
import com.barghest.bux.ui.screens.main.MainViewModel
import com.barghest.bux.ui.screens.transaction.add.AddTransactionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Api() }
    single<TransactionRepository> { TransactionRepository(get()) }
    single<AuthRepository> { AuthRepository(get()) }
    single { TransactionService(get()) }
    single { AuthService(get()) }
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { AddTransactionViewModel(get()) }
}