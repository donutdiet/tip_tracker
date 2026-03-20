package com.example.tiptracker.ui.tabs.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LogsUiState(
    val logs: List<Log> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class LogsViewModel(
    logsRepository: LogRepository
): ViewModel() {

    val uiState = logsRepository.getAllLogs()
        .map { LogsUiState(logs = it, isLoading = false) }
        .catch { e ->
            emit(
                LogsUiState(
                    logs = emptyList(),
                    isLoading = false,
                    errorMessage = "Couldn't load logs. Please try again."
                )
            )
            android.util.Log.e("LogsViewModel", "Error loading logs", e)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogsUiState()
        )
}