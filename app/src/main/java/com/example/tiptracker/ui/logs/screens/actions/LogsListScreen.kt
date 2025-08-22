package com.example.tiptracker.ui.logs.screens.actions

import LogListItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.ui.theme.TipTrackerTheme

@Composable
fun LogsListScreen(
    logs: List<Log>,
    onItemClick: (Int) -> Unit,
    onAddLogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        if (logs.isEmpty()) {
            Text("No logs found. Start adding some!")
        } else {
            LazyColumn {
                items(logs, key = { it.id }) { log ->
                    LogListItem(
                        log = log,
                        onItemClick = { logId -> onItemClick(logId) }
                    )
                }
            }
        }
        CustomFloatingActionButton(
            onClick = onAddLogClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new dining log"
                )
                Text("New Log")
            }
        }
    }
}

@Composable
fun CustomFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TipTrackerTheme {
        LogsListScreen(
            logs = emptyList(),
            onItemClick = {},
            onAddLogClick = {}
        )
    }
}