package com.barghest.bux.ui.screens.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Transaction(val id: Int, val type: String, val amount: Double)

class MainViewModel : ViewModel() {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    fun addTransaction(type: String, amount: Double) {
        val newItem = Transaction(_transactions.value.size + 1, type, amount)
        _transactions.value += newItem
    }
}