package com.example.tiptracker.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.tiptracker.data.Log
import com.example.tiptracker.data.LogsRepository
import com.example.tiptracker.usecase.ValidateMoney

class LogFormViewModel(private val logsRepository: LogsRepository) : ViewModel() {

    var formState by mutableStateOf(LogFormUiState())
        private set

    fun updateUiState(logFormState: LogFormState) {
        formState = LogFormUiState(logFormState = logFormState, isValid = validateForm(logFormState))
    }

    fun clearState() {
        formState = LogFormUiState()
    }

    suspend fun saveLog() {
        if(validateForm()) {
            logsRepository.insertLog(formState.logFormState.toLog())
        }
    }

    private fun validateForm(logFormState: LogFormState = formState.logFormState): Boolean {
        return ValidateMoney.execute(logFormState.bill)
    }
}

data class LogFormUiState(
    val logFormState: LogFormState = LogFormState(),
    val isValid: Boolean = false
)

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