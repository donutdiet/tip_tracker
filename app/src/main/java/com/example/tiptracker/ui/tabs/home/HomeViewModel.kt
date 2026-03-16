package com.example.tiptracker.ui.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class HomeUiState(
    val billAmount: String = "",
    val tipPercent: String = "",
    val partySize: String = "",
    val roundUpTip: Boolean = false,
    val roundUpTotal: Boolean = false,
    val restaurantName: String = "",
    val review: String = "",
    val date: String = LocalDate.now().toString(),
    val rating: Double = 5.0,
    val isSaving: Boolean = false,
) {
    val tipAmount: Double
        get() {
            val bill = billAmount.toDoubleOrNull() ?: 0.0
            val tipPercent = tipPercent.toDoubleOrNull() ?: 0.0
            val rawTip = bill * (tipPercent / 100.0)

            return when {
                roundUpTip -> ceil(rawTip)
                roundUpTotal -> {
                    val rawTotal = bill + rawTip
                    val roundedTotal = ceil(rawTotal)
                    roundedTotal - bill
                }
                else -> rawTip
            }.roundToTwoDecimals()
        }

    val total: Double
        get() {
            val bill = billAmount.toDoubleOrNull() ?: 0.0
            return (bill + tipAmount).roundToTwoDecimals()
        }
}

sealed interface HomeAction {
    data class onBillAmountChange(val billAmount: String) : HomeAction
    data class onTipPercentChange(val tipPercent: String) : HomeAction
    data class onPartySizeChange(val partySize: String) : HomeAction
    data object onRoundUpTipToggle : HomeAction
    data object onRoundUpTotalToggle : HomeAction
    data class onRestaurantNameChange(val restaurantName: String) : HomeAction
    data class onReviewChange(val review: String) : HomeAction
    data class onRatingChange(val rating: Double) : HomeAction
    data object onSaveLog : HomeAction
    data object onClearLog : HomeAction
}

sealed interface HomeEvent {
    data object LogSaved : HomeEvent
    data class ShowError(val message: String) : HomeEvent
}

class HomeViewModel(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.onBillAmountChange -> { _uiState.update { it.copy(billAmount = action.billAmount) }}
            is HomeAction.onTipPercentChange -> { _uiState.update { it.copy(tipPercent = action.tipPercent) }}
            is HomeAction.onPartySizeChange -> { _uiState.update { it.copy(partySize = action.partySize) }}
            is HomeAction.onRoundUpTipToggle -> {
                _uiState.update {
                    it.copy(roundUpTip = !it.roundUpTip, roundUpTotal = false)
                }
            }
            is HomeAction.onRoundUpTotalToggle -> {
                _uiState.update {
                    it.copy(roundUpTotal = !it.roundUpTotal, roundUpTip = false)
                }
            }
            is HomeAction.onRestaurantNameChange -> { _uiState.update { it.copy(restaurantName = action.restaurantName) }}
            is HomeAction.onReviewChange -> { _uiState.update { it.copy(review = action.review) }}
            is HomeAction.onRatingChange -> { _uiState.update { it.copy(rating = action.rating) }}
            is HomeAction.onSaveLog -> { saveLog() }
            is HomeAction.onClearLog -> { _uiState.update { it.copy(billAmount = "", tipPercent = "", partySize = "", roundUpTip = false, roundUpTotal = false, restaurantName = "", review = "", rating = 5.0) }}
        }
    }

    fun saveLog() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val log = Log(
                    bill = _uiState.value.billAmount.toDoubleOrNull() ?: 0.0,
                    tipPercent = _uiState.value.tipPercent.toDoubleOrNull() ?: 0.0,
                    total = _uiState.value.total,
                    partySize = _uiState.value.partySize.toIntOrNull() ?: 1,
                    restaurantName = _uiState.value.restaurantName,
                    review = _uiState.value.review,
                    rating = _uiState.value.rating,
                    date = _uiState.value.date
                )
                logRepository.insertLog(log)
                _events.send(HomeEvent.LogSaved)
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowError(e.message ?: "An error occurred while saving the log"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}