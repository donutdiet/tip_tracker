package com.example.tiptracker.ui.tabs.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileRoot(
    onLogClick: (Int) -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
   val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfilePage(
        uiState = uiState,
        onLogItemClick = onLogClick
    )
}