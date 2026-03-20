package com.example.tiptracker.ui.features.editlog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.R
import com.example.tiptracker.utils.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogRoot(
    logId: Int,
    onBack: () -> Unit,
    onLogUpdated: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: EditLogViewModel = koinViewModel(parameters = { parametersOf(logId) })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EditLogEvent.LogUpdated -> {
                onLogUpdated()
            }
            is EditLogEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Navigate back to log detail page"
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        EditLogPage(
            uiState = uiState,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}