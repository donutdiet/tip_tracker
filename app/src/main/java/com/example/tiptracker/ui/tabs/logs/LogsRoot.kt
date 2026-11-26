package com.example.tiptracker.ui.tabs.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.ui.features.map.MapsWarmup
import org.koin.androidx.compose.koinViewModel

@Composable
fun LogsRoot(
    onLogClick: (Int) -> Unit,
    onOpenMap: () -> Unit,
    viewModel: LogsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The Map button shows once there are logs: get the Maps SDK loaded before it's tapped
    LaunchedEffect(uiState.logs.isNotEmpty()) {
        if (uiState.logs.isNotEmpty()) MapsWarmup.start(context)
    }

    LogsListPage(
        uiState = uiState,
        onLogClick = onLogClick,
        onOpenMap = onOpenMap
    )
}