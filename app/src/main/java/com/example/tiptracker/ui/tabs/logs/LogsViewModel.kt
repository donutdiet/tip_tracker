package com.example.tiptracker.ui.tabs.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LogsUiState(
    val logs: List<Log> = emptyList(),
    val isLoading: Boolean = true
)

class LogsViewModel(
    logsRepository: LogRepository
): ViewModel() {

    val uiState = logsRepository.getAllLogs()
        .map { LogsUiState(logs = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogsUiState()
        )
}