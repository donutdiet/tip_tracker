package com.example.tiptracker.ui.tabs.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun LogsRoot(
    onLogClick: (Int) -> Unit,
    viewModel: LogsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LogsListPage(
        uiState = uiState,
        onLogClick = onLogClick
    )
}