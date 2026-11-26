package com.example.tiptracker.ui.features.editlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.R
import com.example.tiptracker.ui.components.ConfirmationDialog
import com.example.tiptracker.ui.components.PlaceNameDialog
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
    var showDiscardChangesDialog by remember { mutableStateOf(false) }

    // Intercept system back button if unsaved changes detected
    BackHandler(enabled = uiState.editsMade) {
        showDiscardChangesDialog = true
    }

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

    if (showDiscardChangesDialog) {
        ConfirmationDialog(
            title = "Discard changes?",
            message = "Your edits to this log haven't been saved and will be lost.",
            confirmText = "Discard",
            dismissText = "Keep editing",
            onConfirm = {
                showDiscardChangesDialog = false
                onBack()
            },
            onDismiss = { showDiscardChangesDialog = false },
            icon = R.drawable.edit,
            destructive = true,
            destructiveIcon = false
        )
    }

    uiState.placeNamePrompt?.let { prompt ->
        PlaceNameDialog(
            prompt = prompt,
            onSave = { viewModel.onAction(EditLogAction.PlaceNameChosen(it)) },
            onCancel = { viewModel.onAction(EditLogAction.PlaceNameCancel) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                expandedHeight = 56.dp,
                title = { Text(text = "Edit", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { if (uiState.editsMade) showDiscardChangesDialog = true else onBack() }
                    ) {
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
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        )
    }
}
