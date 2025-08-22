package com.example.tiptracker.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogsRepository
import com.example.tiptracker.ui.logs.screens.Review
import com.example.tiptracker.ui.tabs.TabBarItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface UiIntent {
    data class EnterBill(val value: String): UiIntent
    data class EnterTipPercent(val value: String): UiIntent
    data class EnterPartySize(val value: String): UiIntent
    data class EnterRoundUpTip(val value: Boolean): UiIntent
    data class EnterRoundUpTotal(val value: Boolean): UiIntent
    data class EnterRestaurantName(val value: String): UiIntent
    data class EnterRestaurantDescription(val value: String): UiIntent
    data class EnterDate(val value: LocalDate): UiIntent

    object Clear: UiIntent
    object Log: UiIntent
    object Back: UiIntent
    object Save: UiIntent
}

sealed interface UiEvent {
    data object Idle: UiEvent
    data class Navigate(val route: NavKey): UiEvent
    data object Back: UiEvent
//    data class SwitchTab(val tab: TabBarItem): UiEvent
    data class ShowSnackbar(val message: String): UiEvent
}

class LogFormViewModel(private val logsRepository: LogsRepository) : ViewModel() {
    private var _state = MutableStateFlow(LogFormState())
    val state: StateFlow<LogFormState> = _state.asStateFlow()

    private val _event: Channel<UiEvent> = Channel()
    val event = _event.receiveAsFlow()

    fun onIntent(event: UiIntent) {
        when (event) {
            is UiIntent.EnterBill -> {
                _state.update { it.copy(bill = event.value) }
            }

            is UiIntent.EnterTipPercent -> {
                _state.update { it.copy(tipPercent = event.value) }
            }

            is UiIntent.EnterPartySize -> {
                _state.update { it.copy(partySize = event.value) }
            }

            is UiIntent.EnterRoundUpTip -> {
                _state.update {
                    it.copy(
                        roundUpTip = event.value,
                        roundUpTotal = if (event.value) false else it.roundUpTotal
                    )
                }
            }

            is UiIntent.EnterRoundUpTotal -> {
                _state.update {
                    it.copy(
                        roundUpTotal = event.value,
                        roundUpTip = if (event.value) false else it.roundUpTip
                    )
                }
            }

            is UiIntent.EnterRestaurantName -> {
                _state.update { it.copy(restaurantName = event.value) }
            }

            is UiIntent.EnterRestaurantDescription -> {
                _state.update { it.copy(restaurantDescription = event.value) }
            }

            is UiIntent.EnterDate -> {
                _state.update { it.copy(date = event.value) }
            }

            UiIntent.Clear -> _state.value = LogFormState()
            UiIntent.Back -> viewModelScope.launch {
                _event.send(UiEvent.Back)
            }
            UiIntent.Log -> viewModelScope.launch {
                _event.send(UiEvent.Navigate(Review))
            }
            UiIntent.Save -> viewModelScope.launch {
                saveLog()
            }
        }
    }

    private suspend fun saveLog() {
        try {
            logsRepository.insertLog(_state.value.toLog())
            _state.value = LogFormState()
//            _event.send(UiEvent.SwitchTab(TabBarItem.Logs))
        } catch (e: Exception) {
            _event.send(UiEvent.ShowSnackbar("Save failed: ${e.message ?: "Unknown error"}"))
        }
    }
}

fun LogFormState.toLog(): Log = Log(
    id = id,
    bill = parsedBill,
    tipPercent = trueTipPercent,
    partySize = parsedPartySize,
    tip = calculatedTip,
    total = calculatedTotal,
    perPerson = calculatedPerPerson,
    restaurantName = restaurantName,
    restaurantDescription = restaurantDescription,
    date = date
)