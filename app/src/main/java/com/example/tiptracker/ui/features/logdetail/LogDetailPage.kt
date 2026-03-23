package com.example.tiptracker.ui.features.logdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.formatTipPercent

@Composable
fun LogDetailPage(
    uiState: LogDetailUiState,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp, 4.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = uiState.restaurantName,
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatDateForDisplay(uiState.date),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "·",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = uiState.partySize.toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Review", style = MaterialTheme.typography.bodyLarge)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = uiState.rating.toString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Text(text = uiState.review, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.clickable(
                onClick = { expanded = !expanded }
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Bill", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$${formatCurrency(uiState.bill)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tip",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$${formatCurrency(uiState.tipAmount)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 2.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$${formatCurrency(uiState.total)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (!expanded) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Show more", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (expanded) Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Total per person", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "$${formatCurrency(uiState.totalPerPerson)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Tip Percent", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${formatTipPercent(uiState.tipPercent)}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text("Show less", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LogDetailPagePreview() {
    TipTrackerTheme {
        LogDetailPage(
            uiState = LogDetailUiState(
                bill = 128.12,
                tipPercent = 20.0,
                total = 153.74,
                partySize = 2,
                restaurantName = "The Pearl",
                review = "goysters were so geurch",
                rating = 8.6,
                date = "2026-03-21",
                tipAmount = 25.62,
                totalPerPerson = 76.87,
                isLoading = false,
                isNotFound = false,
                errorMessage = null,
                isDeleting = false
            )
        )
    }
}