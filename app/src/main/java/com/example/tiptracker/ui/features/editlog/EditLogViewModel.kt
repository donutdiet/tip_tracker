package com.example.tiptracker.ui.features.editlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditLogUiState(
    val bill: String = "",
    val tipAmount: String = "",
    val partySize: String = "",
    val restaurantName: String = "",
    val review: String = "",
    val date: String = "",
    val rating: Double = 5.0,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
    val isUpdating: Boolean = false,
    val editsMade: Boolean = false
)

sealed interface EditLogAction {
    data class BillChanged(val bill: String) : EditLogAction
    data class TipAmountChanged(val tipAmount: String) : EditLogAction
    data class PartySizeChanged(val partySize: String) : EditLogAction
    data class RestaurantNameChanged(val restaurantName: String) : EditLogAction
    data class ReviewChanged(val review: String) : EditLogAction
    data class DateChanged(val date: String) : EditLogAction
    data class RatingChanged(val rating: Double) : EditLogAction
    data object Update : EditLogAction
}

sealed interface EditLogEvent {
    data object LogUpdated : EditLogEvent
    data class ShowError(val message: String) : EditLogEvent
}

class EditLogViewModel(
    private val logId: Int,
    private val logsRepository: LogRepository
) : ViewModel() {

    private val _events = Channel<EditLogEvent>()
    val events = _events.receiveAsFlow()

    private val _uiState = MutableStateFlow(EditLogUiState())
    val uiState: StateFlow<EditLogUiState> = _uiState.asStateFlow()

    // Keep track of the state as it was when loaded from the database
    private var initialUiState: EditLogUiState? = null

    init {
        loadLog()
    }

    fun onAction(action: EditLogAction) {
        when (action) {
            EditLogAction.Update -> updateLog()
            else -> {
                _uiState.update { currentState ->
                    val updatedState = when (action) {
                        is EditLogAction.BillChanged -> currentState.copy(bill = action.bill)
                        is EditLogAction.TipAmountChanged -> currentState.copy(tipAmount = action.tipAmount)
                        is EditLogAction.PartySizeChanged -> currentState.copy(partySize = action.partySize)
                        is EditLogAction.RestaurantNameChanged -> currentState.copy(restaurantName = action.restaurantName)
                        is EditLogAction.ReviewChanged -> currentState.copy(review = action.review)
                        is EditLogAction.DateChanged -> currentState.copy(date = action.date)
                        is EditLogAction.RatingChanged -> currentState.copy(rating = action.rating)
                    }
                    // Calculate if the form is actually different from the original data
                    updatedState.copy(editsMade = checkIfEditsMade(updatedState))
                }
            }
        }
    }

    private fun loadLog() {
        viewModelScope.launch {
            try {
                val log = logsRepository.getLogById(logId).first()
                if (log == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNotFound = true
                        )
                    }
                    return@launch
                }

                val loadedState = EditLogUiState(
                    isLoading = false,
                    bill = log.bill.toString(),
                    tipAmount = (log.total - log.bill).roundToTwoDecimals().toString(),
                    partySize = log.partySize.toString(),
                    restaurantName = log.restaurantName,
                    review = log.review,
                    date = log.date,
                    rating = log.rating,
                    errorMessage = null,
                    editsMade = false
                )

                initialUiState = loadedState
                _uiState.value = loadedState

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load log details. Please try again."
                    )
                }
                android.util.Log.e("EditLogViewModel", "Error loading log detail", e)
            }
        }
    }

    private fun updateLog() {
        if (_uiState.value.isUpdating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                val state = _uiState.value
                val bill = state.bill.toDoubleOrNull() ?: 0.0
                val tipAmount = state.tipAmount.toDoubleOrNull() ?: 0.0
                val partySize = state.partySize.toIntOrNull() ?: 1

                if (bill < 0.0 || tipAmount < 0.0 || partySize < 1) {
                    _events.send(EditLogEvent.ShowError("Please enter a valid bill, tip amount, and party size."))
                    return@launch
                }

                val total = (bill + tipAmount).roundToTwoDecimals()
                val tipPercent = if (bill != 0.0) {
                    ((tipAmount / bill) * 100.0).roundToTwoDecimals()
                } else {
                    0.0
                }

                logsRepository.updateLog(
                    Log(
                        id = logId,
                        bill = bill,
                        tipPercent = tipPercent,
                        total = total,
                        partySize = partySize,
                        restaurantName = state.restaurantName,
                        review = state.review,
                        rating = state.rating,
                        date = state.date
                    )
                )
                _events.send(EditLogEvent.LogUpdated)
            } catch (e: Exception) {
                _events.send(EditLogEvent.ShowError("Couldn't update your log. Please try again."))
                android.util.Log.e("EditLogViewModel", "Error updating log $logId", e)
            } finally {
                _uiState.update { it.copy(isUpdating = false) }
            }
        }
    }

    private fun checkIfEditsMade(state: EditLogUiState): Boolean {
        val initial = initialUiState ?: return false
        return state.bill != initial.bill ||
                state.tipAmount != initial.tipAmount ||
                state.partySize != initial.partySize ||
                state.restaurantName != initial.restaurantName ||
                state.review != initial.review ||
                state.date != initial.date ||
                state.rating != initial.rating
    }
}