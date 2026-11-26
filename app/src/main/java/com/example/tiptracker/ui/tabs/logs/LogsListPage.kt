package com.example.tiptracker.ui.tabs.logs

import com.example.tiptracker.ui.theme.card
import com.example.tiptracker.ui.theme.floating
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.titleMediumMono
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.formatTipPercent

@Composable
fun LogsListPage(
    uiState: LogsUiState,
    onLogClick: (Int) -> Unit,
    onOpenMap: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.logs.isEmpty() -> {
                Text(
                    text = "No logs saved",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Room below the last log so it can scroll clear of the Map button
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = MAP_BUTTON_CLEARANCE
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.logs, key = { it.id }) { log ->
                        LogItem(
                            log = log,
                            onLogItemClick = { onLogClick(log.id) }
                        )
                    }
                }
                MapButton(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

/** A floating pill that opens the map of the user's places, styled like Google Maps' buttons. */
@Composable
private fun MapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.floating,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.map),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Map", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// The Map button's height plus its margin, with a little extra
private val MAP_BUTTON_CLEARANCE = 80.dp

@Composable
private fun LogItem(
    log: Log,
    onLogItemClick: () -> Unit
) {
    Card(
        onClick = onLogItemClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.card,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.restaurantName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "$${formatCurrency(log.total)}",
                    style = MaterialTheme.typography.titleMediumMono
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${log.partySize}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${log.rating}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.hand_meal),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${formatTipPercent(log.tipPercent)}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(
                    text = formatDateForDisplay(log.date),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogsListPagePreview() {
    TipTrackerTheme {
        LogsListPage(
            uiState = LogsUiState(
                isLoading = false,
                logs = listOf(
                    Log(
                        id = 1,
                        bill = 132.19,
                        tipPercent = 15.0,
                        total = 152.02,
                        partySize = 2,
                        restaurantName = "The Pearl sd flsdkfj sdlkfj sldfkj sdlfksdj flksdj fdsl",
                        review = "goysters",
                        rating = 8.6,
                        date = "2023-07-09"
                    ),
                    Log(
                        id = 2,
                        bill = 45.50,
                        tipPercent = 20.0,
                        total = 54.60,
                        partySize = 1,
                        restaurantName = "Taco Bell",
                        review = "Fast",
                        rating = 5.0,
                        date = "2023-07-10"
                    )
                )
            ),
            onLogClick = {},
            onOpenMap = {}
        )
    }
}
