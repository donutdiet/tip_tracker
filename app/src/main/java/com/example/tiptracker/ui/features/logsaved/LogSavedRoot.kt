package com.example.tiptracker.ui.features.logsaved

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LogSavedRoot(
    logId: Int,
    viewModel: LogSavedViewModel = koinViewModel(parameters = { parametersOf(logId) })
) {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(text = "Log saved successfully!")
            Text(text = "SavedLogId = $logId ")
        }
    }
}