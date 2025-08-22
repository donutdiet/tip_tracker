package com.example.tiptracker.ui.logs.screens.actions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEditScreen(
    logId: Int,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Text("Editing log $logId")
}

@Preview(showBackground = true)
@Composable
fun LogEditScreenPreview() {
    MaterialTheme {
        LogDetailsScreen(
            logId = 12,
            onBackClick = {},
            onEditClick = {}
        )
    }
}