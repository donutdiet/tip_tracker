package com.example.tiptracker.ui.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatTipPercent
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class HomeUiState(
    val billAmount: String = "",
    val tipPreset1: Boolean = false,
    val tipPreset2: Boolean = false,
    val tipPercent: String = "",
    val partySize: String = "",
    val roundUpTip: Boolean = false,
    val roundUpTotal: Boolean = false,
    val restaurantName: String = "",
    val review: String = "",
    val date: String = LocalDate.now().toString(),
    val rating: Double = 5.0,
    val isSaving: Boolean = false,
    val tipPreset1Percent: Int = 10,
    val tipPreset2Percent: Int = 15,
) {
    // Text displayed in the custom tip text field
    val displayedTipPercent: String
        get() {
            return if (tipPreset1) {
                ""
            } else if (tipPreset2) {
                ""
            } else {
                tipPercent
            }
        }

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

    val trueTipPercent: Double
        get() {
            if (tipAmount == 0.0) return 0.0

            val percent = tipPercent.toDoubleOrNull() ?: 0.0
            if (!roundUpTip && !roundUpTotal) return percent.roundToTwoDecimals()

            return (100 * tipAmount / (total - tipAmount)).roundToTwoDecimals()
        }

    val formattedTipAmount: String
        get() = formatCurrency(tipAmount)

    val formattedTotal: String
        get() = formatCurrency(total)

    val formattedTotalPerPerson: String
        get() {
            val party = partySize.toDoubleOrNull() ?: 0.0
            val perPerson = (total / party).roundToTwoDecimals()
            return formatCurrency(perPerson)
        }

    val formattedTrueTipPercent: String
        get() = formatTipPercent(trueTipPercent)
}

sealed interface HomeAction {
    data class onBillAmountChange(val billAmount: String) : HomeAction
    data object onTipPreset1Change : HomeAction
    data object onTipPreset2Change : HomeAction
    data class onTipPercentChange(val tipPercent: String) : HomeAction
    data class onPartySizeChange(val partySize: String) : HomeAction
    data object onRoundUpTipToggle : HomeAction
    data object onRoundUpTotalToggle : HomeAction
    data class onRestaurantNameChange(val restaurantName: String) : HomeAction
    data class onReviewChange(val review: String) : HomeAction
    data class onDateChange(val date: String) : HomeAction
    data class onRatingChange(val rating: Double) : HomeAction
    data object saveLog : HomeAction
    data object clear : HomeAction
}

sealed interface HomeEvent {
    data object LogSaved : HomeEvent
    data class ShowError(val message: String) : HomeEvent
}

class HomeViewModel(
    private val logRepository: LogRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = combine(
        _uiState,
        settingsRepository.tipPreset1Percent,
        settingsRepository.tipPreset2Percent
    ) { state, p1, p2 ->
        state.copy(
            tipPreset1Percent = p1,
            tipPreset2Percent = p2
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.onBillAmountChange -> _uiState.update { it.copy(billAmount = action.billAmount) }
            is HomeAction.onTipPreset1Change -> {
                val preset1Percent = uiState.value.tipPreset1Percent
                _uiState.update {
                    it.copy(
                        tipPreset1 = !it.tipPreset1,
                        tipPreset2 = false,
                        tipPercent = preset1Percent.toString()
                    )
                }
            }
            is HomeAction.onTipPreset2Change -> {
                val preset2Percent = uiState.value.tipPreset2Percent
                _uiState.update {
                    it.copy(
                        tipPreset2 = !it.tipPreset2,
                        tipPreset1 = false,
                        tipPercent = preset2Percent.toString()
                    )
                }
            }
            is HomeAction.onTipPercentChange -> {
                _uiState.update {
                    it.copy(
                        tipPercent = action.tipPercent,
                        tipPreset1 = false,
                        tipPreset2 = false
                    )
                }
            }
            is HomeAction.onPartySizeChange -> _uiState.update { it.copy(partySize = action.partySize) }
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
            is HomeAction.onRestaurantNameChange -> _uiState.update { it.copy(restaurantName = action.restaurantName) }
            is HomeAction.onReviewChange -> _uiState.update { it.copy(review = action.review) }
            is HomeAction.onDateChange -> _uiState.update { it.copy(date = action.date) }
            is HomeAction.onRatingChange -> _uiState.update { it.copy(rating = action.rating) }
            is HomeAction.saveLog -> saveLog()
            is HomeAction.clear -> clearState()
        }
    }

    private fun saveLog() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val log = Log(
                    bill = _uiState.value.billAmount.toDoubleOrNull() ?: 0.0,
                    tipPercent = _uiState.value.trueTipPercent,
                    total = _uiState.value.total,
                    partySize = _uiState.value.partySize.toIntOrNull() ?: 1,
                    restaurantName = _uiState.value.restaurantName,
                    review = _uiState.value.review,
                    rating = _uiState.value.rating,
                    date = _uiState.value.date
                )
                logRepository.insertLog(log)
                clearState()
                _events.send(HomeEvent.LogSaved)
            } catch (e: Exception) {
                val userMessage = when (e) {
                    is android.database.sqlite.SQLiteFullException -> "Storage is full. Please free some space."
                    else -> "Couldn't save your log. Please try again."
                }
                _events.send(HomeEvent.ShowError(userMessage))
                android.util.Log.e("HomeViewModel", "Error saving log", e)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun clearState() {
        _uiState.value = HomeUiState()
    }
}
