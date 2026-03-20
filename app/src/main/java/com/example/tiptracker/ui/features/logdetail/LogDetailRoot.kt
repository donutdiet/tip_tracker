package com.example.tiptracker.ui.features.logdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.R
import com.example.tiptracker.utils.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailRoot(
    logId: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onLogDeleted: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: LogDetailViewModel = koinViewModel(parameters = { parametersOf(logId) })
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LogDetailEvent.LogDeleted -> {
                onLogDeleted()
            }
            is LogDetailEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Navigate back to log list"
                        )
                    }
                },
                actions = {
                    if(!uiState.isLoading && !uiState.isNotFound && uiState.errorMessage == null) {
                        IconButton(onClick = { onEdit(logId) }) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = "Edit"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !uiState.isDeleting
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LogDetailPage(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(text = "Delete log?", style = MaterialTheme.typography.bodyLarge)
            },
            text = {
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.onAction(LogDetailAction.Delete)
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }
}
