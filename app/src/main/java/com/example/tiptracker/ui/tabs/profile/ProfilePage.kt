package com.example.tiptracker.ui.tabs.profile

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
import com.example.tiptracker.ui.components.RatingDistributionGraph
import com.example.tiptracker.ui.theme.ScreenPadding
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.titleMediumMono
import com.example.tiptracker.ui.theme.titleSmallMono
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.formatTipPercent
import kotlinx.coroutines.delay

@Composable
fun ProfilePage(
    uiState: ProfileUiState,
    onLogItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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

            else -> {
                ProfilePageContent(uiState, onLogItemClick)
            }
        }
    }
}

@Composable
fun ProfilePageContent(
    uiState: ProfileUiState,
    onLogItemClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(expanded) {
        if (expanded) {
            // Ensure expanded receipt content is brought fully into view.
            delay(125)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(paddingValues = ScreenPadding)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Rating distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.star),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${uiState.logStats.avgRating} average",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        RatingDistributionGraph(distribution = uiState.ratingDistribution)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Awards",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Icon(
                painter = painterResource(R.drawable.trophy),
                contentDescription = null
            )
        }

        uiState.awards.allAwards.forEach { award ->
            AwardItem(
                log = award.log,
                award = award.title,
                metric = award.metric,
                onLogItemClick = onLogItemClick,
                metricIcon = award.iconRes
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Average Receipt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${uiState.logStats.totalLogs} logs",
                style = MaterialTheme.typography.labelLarge
            )
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
                    Text(text = "Bill", style = MaterialTheme.typography.titleMediumMono)
                    Text(
                        text = "$${formatCurrency(uiState.logStats.avgBill)}",
                        style = MaterialTheme.typography.titleMediumMono
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Tip", style = MaterialTheme.typography.titleMediumMono)
                    Text(
                        text = "$${formatCurrency(uiState.logStats.avgTipAmount)}",
                        style = MaterialTheme.typography.titleMediumMono
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
                    Text(text = "Total", style = MaterialTheme.typography.titleMediumMono)
                    Text(
                        text = "$${formatCurrency(uiState.logStats.avgTotal)}",
                        style = MaterialTheme.typography.titleMediumMono
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
                            Text(text = "Total per person", style = MaterialTheme.typography.titleSmallMono)
                            Text(
                                text = "$${formatCurrency(uiState.logStats.avgTotalPerPerson)}",
                                style = MaterialTheme.typography.titleSmallMono
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Tip percent", style = MaterialTheme.typography.titleSmallMono)
                            Text(
                                text = "${formatTipPercent(uiState.logStats.avgTipPercent)}%",
                                style = MaterialTheme.typography.titleSmallMono
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Party size", style = MaterialTheme.typography.titleSmallMono)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${uiState.logStats.avgPartySize}",
                                    style = MaterialTheme.typography.titleSmallMono
                                )
                            }
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
    }
}

@Composable
fun AwardItem(
    log: Log,
    award: String,
    metric: String,
    onLogItemClick: (Int) -> Unit,
    @DrawableRes metricIcon: Int? = null
) {
    Card(
        onClick = { onLogItemClick(log.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
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
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDateForDisplay(log.date),
                    style = MaterialTheme.typography.labelMedium
                )
            }

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
                            painter = painterResource(R.drawable.trophy),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = award,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (metricIcon != null) {
                        Icon(
                            painter = painterResource(metricIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = metric,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
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
                    RatingCount(rating = 0.0, count = 2),
                    RatingCount(rating = 0.5, count = 1),
                    RatingCount(rating = 1.0, count = 3),
                    RatingCount(rating = 1.5, count = 2),
                    RatingCount(rating = 2.0, count = 2),
                    RatingCount(rating = 2.5, count = 3),
                    RatingCount(rating = 3.0, count = 4),
                    RatingCount(rating = 3.5, count = 2),
                    RatingCount(rating = 4.0, count = 5),
                    RatingCount(rating = 4.5, count = 3),
                    RatingCount(rating = 5.0, count = 6),
                    RatingCount(rating = 5.5, count = 4),
                    RatingCount(rating = 6.0, count = 7),
                    RatingCount(rating = 6.5, count = 5),
                    RatingCount(rating = 7.0, count = 8),
                    RatingCount(rating = 7.5, count = 6),
                    RatingCount(rating = 8.0, count = 9),
                    RatingCount(rating = 8.5, count = 7),
                    RatingCount(rating = 9.0, count = 6),
                    RatingCount(rating = 9.5, count = 4),
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
            onLogItemClick = {}
        )
    }
}