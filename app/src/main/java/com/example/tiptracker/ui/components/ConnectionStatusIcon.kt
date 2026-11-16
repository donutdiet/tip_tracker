package com.example.tiptracker.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.tiptracker.R
import com.example.tiptracker.data.repository.ConnectionStatus

/**
 * Top-bar indicator for connectivity problems. Renders nothing while online.
 * Add a branch per [ConnectionStatus] as new states are introduced.
 */
@Composable
fun ConnectionStatusIcon(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    when (status) {
        ConnectionStatus.Online -> Unit
        ConnectionStatus.Offline -> IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(id = R.drawable.offline),
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
