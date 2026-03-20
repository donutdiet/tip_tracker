package com.example.tiptracker.ui.tabs.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
import com.example.tiptracker.ui.components.RatingDistributionGraph
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatTipPercent

@Composable
fun ProfilePage(
    uiState: ProfileUiState,
    onLogClick: (Int) -> Unit,
    modifier: Modifier = Modifier
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
                Text(text = uiState.errorMessage)
            }

            else -> {
                ProfilePageContent(uiState, onLogClick)
            }
        }
    }
}

@Composable
fun ProfilePageContent(
    uiState: ProfileUiState,
    onLogClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Average Receipt")
            Text(text = "${uiState.logStats.totalLogs} logs")
        }
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
                    Text(text = "Bill")
                    Text(text = "$${formatCurrency(uiState.logStats.avgBill)}")
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Tip")
                    Text(text = "$${formatCurrency(uiState.logStats.avgTipAmount)}")
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
                    Text(text = "Total")
                    Text(text = "$${formatCurrency(uiState.logStats.avgTotal)}")
                }

                if (!expanded) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Show more")
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
                            Text(text = "Total per person")
                            Text(text = "$${formatCurrency(uiState.logStats.avgTotalPerPerson)}")
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Tip percent")
                            Text(text = "${formatTipPercent(uiState.logStats.avgTipPercent)}%")
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Party size")
                            Text(text = "${uiState.logStats.avgPartySize}")
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text("Show less")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        RatingDistributionGraph(distribution = uiState.ratingDistribution)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    TipTrackerTheme {
        ProfilePage(
            uiState = ProfileUiState(
                logStats = LogStats(
                    totalLogs = 24,
                    avgBill = 48.32,
                    avgTipPercent = 18.4,
                    avgTotal = 57.21,
                    avgPartySize = 2.6,
                    avgRating = 8.1,
                    avgTipAmount = 8.89,
                    avgTotalPerPerson = 23.67
                ),
                ratingDistribution = listOf(
                    RatingCount(rating = 5.0, count = 2),
                    RatingCount(rating = 6.0, count = 3),
                    RatingCount(rating = 7.0, count = 5),
                    RatingCount(rating = 8.0, count = 7),
                    RatingCount(rating = 9.0, count = 4),
                    RatingCount(rating = 10.0, count = 3)
                ),
                awards = AwardsUiState(
                    highestSpendPerPerson = Log(
                        id = 7,
                        bill = 184.20,
                        tipPercent = 20.0,
                        total = 221.04,
                        partySize = 2,
                        restaurantName = "Saffron House",
                        review = "Tasting menu was excellent and service was top-tier.",
                        rating = 9.4,
                        date = "2026-02-14"
                    ),
                    mostGenerousTip = Log(
                        id = 13,
                        bill = 42.50,
                        tipPercent = 28.0,
                        total = 54.40,
                        partySize = 3,
                        restaurantName = "Noodle Nook",
                        review = "Super friendly staff and quick service.",
                        rating = 8.8,
                        date = "2026-01-08"
                    ),
                    largestPartySize = Log(
                        id = 4,
                        bill = 212.00,
                        tipPercent = 18.0,
                        total = 250.16,
                        partySize = 8,
                        restaurantName = "The Boardwalk Grill",
                        review = "Big group dinner after game night.",
                        rating = 7.9,
                        date = "2025-12-03"
                    ),
                    topRated = Log(
                        id = 22,
                        bill = 96.75,
                        tipPercent = 22.0,
                        total = 118.04,
                        partySize = 2,
                        restaurantName = "Juniper",
                        review = "Perfect date night spot.",
                        rating = 10.0,
                        date = "2026-03-02"
                    ),
                    lengthiestReview = Log(
                        id = 19,
                        bill = 58.30,
                        tipPercent = 19.0,
                        total = 69.38,
                        partySize = 2,
                        restaurantName = "Harbor Kitchen",
                        review = "Great seafood platter, crisp drinks, and a relaxed patio atmosphere. Would come back for sunset views and the crab cakes.",
                        rating = 8.7,
                        date = "2026-02-01"
                    )
                ),
                isLoading = false
            ),
            onLogClick = {}
        )
    }
}