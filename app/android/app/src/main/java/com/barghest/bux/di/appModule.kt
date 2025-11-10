package com.barghest.bux.di

import com.barghest.bux.data.network.Api
import com.barghest.bux.data.repository.TransactionRepository
import com.barghest.bux.domain.service.TransactionService
import com.barghest.bux.ui.screens.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Api() }
    single<TransactionRepository> { TransactionRepository(get()) }
    single { TransactionService(get()) }
    viewModel { MainViewModel(get()) }
}