package com.example.tiptracker.ui.features.logsaved

import androidx.lifecycle.ViewModel
import com.example.tiptracker.data.repository.LogImageRepository

class LogSavedViewModel(
    private val logId: Int,
    private val repository: LogImageRepository
) : ViewModel() {

}