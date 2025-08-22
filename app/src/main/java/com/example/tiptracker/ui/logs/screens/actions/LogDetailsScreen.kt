package com.example.tiptracker.ui.logs.screens.actions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailsScreen(
    logId: Int,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true
) {
    Column {
        Text("Viewing log $logId details")
        Button(onClick = onEditClick) {
            Text("Edit")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogDetailsScreen() {
    MaterialTheme {
        LogDetailsScreen(
            logId = 12,
            onBackClick = {},
            onEditClick = {}
        )
    }
}