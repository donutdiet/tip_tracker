package com.example.tiptracker.ui.features.logdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.throwSQLiteException
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogDetailUiState(
    val bill: Double = 0.0,
    val tipPercent: Double = 0.0,
    val total: Double = 0.0,
    val partySize: Int = 1,
    val restaurantName: String = "",
    val review: String = "",
    val rating: Double = 0.0,
    val date: String = "",
    val tipAmount: Double = 0.0,
    val totalPerPerson: Double = 0.0,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false
)

sealed interface LogDetailAction {
    data object Delete : LogDetailAction
}

sealed interface LogDetailEvent {
    data object LogDeleted : LogDetailEvent
    data class ShowError(val message: String) : LogDetailEvent
}

class LogDetailViewModel(
    private val logId: Int,
    private val logsRepository: LogRepository
) : ViewModel() {
    private val _events = Channel<LogDetailEvent>()
    val events = _events.receiveAsFlow()

    private val isDeleting = MutableStateFlow(false)
    private val logFlow = logsRepository.getLogById(logId)

    val uiState = combine(
        isDeleting,
        logFlow
            .map { log ->
                if (log == null) {
                    LogDetailUiState(
                        isLoading = false,
                        isNotFound = true
                    )
                } else {
                    val tipAmount = (log.total - log.bill).roundToTwoDecimals()
                    val totalPerPerson = if (log.partySize > 0) {
                        (log.total / log.partySize).roundToTwoDecimals()
                    } else {
                        0.0
                    }

                    LogDetailUiState(
                        isLoading = false,
                        bill = log.bill,
                        tipPercent = log.tipPercent,
                        total = log.total,
                        partySize = log.partySize,
                        restaurantName = log.restaurantName,
                        review = log.review,
                        rating = log.rating,
                        date = log.date,
                        tipAmount = tipAmount,
                        totalPerPerson = totalPerPerson
                    )
                }
            }
            .catch { e ->
                emit(
                    LogDetailUiState(
                        isLoading = false,
                        errorMessage = "Couldn't load log details. Please try again."
                    )
                )
                android.util.Log.e("LogDetailViewModel", "Error loading log detail", e)
            }
    ) { deleting, state ->
        state.copy(isDeleting = deleting)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogDetailUiState()
        )

    fun onAction(action: LogDetailAction) {
        when (action) {
            LogDetailAction.Delete -> deleteLog()
        }
    }

    private fun deleteLog() {
        if (isDeleting.value) return
        isDeleting.update { true }
        android.util.Log.d("LogDetailViewModel", "deleting")

        viewModelScope.launch {
            try {
                val deletedCount = logsRepository.deleteLogById(logId)
                if (deletedCount == 0) {
                    _events.send(LogDetailEvent.ShowError("This log no longer exists."))
                } else {
                    _events.send(LogDetailEvent.LogDeleted)
                }
            } catch (e: Exception) {
                _events.send(LogDetailEvent.ShowError("Couldn't delete your log. Please try again."))
                android.util.Log.e("LogDetailViewModel", "Error deleting log $logId", e)
            } finally {
                isDeleting.update { false }
            }
        }
    }
}