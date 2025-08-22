package com.example.tiptracker.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.repository.LogsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogsViewModel(private val logsRepository: LogsRepository) : ViewModel() {
    private val _logs = MutableStateFlow<List<Log>>(emptyList())
    val logs: StateFlow<List<Log>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            logsRepository.getAllLogsStream().collectLatest { collectedLogs ->
                _logs.value = collectedLogs
            }
        }
    }



}