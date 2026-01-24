package com.barghest.bux.ui.screens.investments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barghest.bux.data.repository.InvestmentRepository
import com.barghest.bux.domain.model.NewTrade
import com.barghest.bux.domain.model.Security
import com.barghest.bux.domain.model.TradeSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddTradeViewModel(
    savedStateHandle: SavedStateHandle,
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val portfolioId: Int = savedStateHandle.get<Int>("portfolioId") ?: 0

    var uiState by mutableStateOf(AddTradeUiState())
        private set

    private val _securities = MutableStateFlow<List<Security>>(emptyList())
    val securities: StateFlow<List<Security>> = _securities.asStateFlow()

    fun updateSide(side: TradeSide) {
        uiState = uiState.copy(side = side)
    }

    fun updateSecurity(security: Security) {
        uiState = uiState.copy(selectedSecurity = security)
    }

    fun updateQuantity(quantity: String) {
        uiState = uiState.copy(quantityText = quantity)
    }

    fun updatePrice(price: String) {
        uiState = uiState.copy(priceText = price)
    }

    fun updateFee(fee: String) {
        uiState = uiState.copy(feeText = fee)
    }

    fun updateNote(note: String) {
        uiState = uiState.copy(note = note)
    }

    fun searchSecurities(query: String) {
        if (query.length < 2) {
            _securities.value = emptyList()
            return
        }

        viewModelScope.launch {
            investmentRepository.searchSecurities(query)
                .onSuccess { _securities.value = it }
                .onFailure { _securities.value = emptyList() }
        }
    }

    fun save(onSuccess: () -> Unit) {
        val security = uiState.selectedSecurity
        if (security == null) {
            uiState = uiState.copy(error = "Выберите ценную бумагу")
            return
        }

        val quantity = uiState.quantityText.toBigDecimalOrNull()
        if (quantity == null || quantity <= BigDecimal.ZERO) {
            uiState = uiState.copy(error = "Введите корректное количество")
            return
        }

        val price = uiState.priceText.toBigDecimalOrNull()
        if (price == null || price <= BigDecimal.ZERO) {
            uiState = uiState.copy(error = "Введите корректную цену")
            return
        }

        val fee = uiState.feeText.toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModelScope.launch {
            uiState = uiState.copy(loading = true, error = null)

            val trade = NewTrade(
                portfolioId = portfolioId,
                securityId = security.id,
                side = uiState.side,
                quantity = quantity,
                price = price,
                fee = fee,
                tradeDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "T00:00:00Z",
                note = uiState.note.ifBlank { null }
            )

            investmentRepository.createTrade(trade)
                .onSuccess {
                    uiState = uiState.copy(loading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        loading = false,
                        error = e.message ?: "Ошибка создания сделки"
                    )
                }
        }
    }
}

data class AddTradeUiState(
    val side: TradeSide = TradeSide.BUY,
    val selectedSecurity: Security? = null,
    val quantityText: String = "",
    val priceText: String = "",
    val feeText: String = "0",
    val note: String = "",
    val loading: Boolean = false,
    val error: String? = null
)
