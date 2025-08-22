package com.example.tiptracker.ui_old

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui_old.UserStats.largestPartySizeLogId
import com.example.tiptracker.ui_old.UserStats.highestSpendLogId
import com.example.tiptracker.ui_old.UserStats.highestTipPercentLogId
import com.example.tiptracker.ui_old.addentry.LogViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    logViewModel: LogViewModel,
    modifier: Modifier = Modifier
) {
    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Dining Stats",
                style = MaterialTheme.typography.displayMedium
            )
            UserStatBoard(
                currencyFormatter = currencyFormatter,
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 40.dp)
            )

            val highestSpendLog = remember(logViewModel.diningLogs, highestSpendLogId) {
                logViewModel.diningLogs.find { it.id == highestSpendLogId }
            }
            val highestTipPercentLog = remember(logViewModel.diningLogs, highestTipPercentLogId) {
                logViewModel.diningLogs.find { it.id == highestTipPercentLogId }
            }
            val largestPartySizeLog = remember(logViewModel.diningLogs, largestPartySizeLogId) {
                logViewModel.diningLogs.find { it.id == largestPartySizeLogId }
            }

            DiningLogRecord(
                log = highestSpendLog,
                recordLabel = "Highest Spend (per person)",
                recordStat = highestSpendLog?.let {
                    currencyFormatter.format(it.totalAmountPerPerson)
                } ?: "N/A",
                secondaryStat = "(x${highestSpendLog?.personCount.toString()})",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
            DiningLogRecord(
                log = highestTipPercentLog,
                recordLabel = "Highest Tip Percent",
                recordStat = String.format(
                    Locale.getDefault(),
                    "%.2f",
                    highestTipPercentLog?.tipPercent
                ) + "%",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
            DiningLogRecord(
                log = largestPartySizeLog,
                recordLabel = "Largest Party Size",
                recordStat = largestPartySizeLog?.personCount.toString(),
                recordStatIconId = R.drawable.person,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun UserStatBoard(
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        UserStatRow(
            label = "Total Spend",
            value = currencyFormatter.format(UserStats.totalSpend),
        )
        UserStatRow(
            label = "Total Tips",
            value = currencyFormatter.format(UserStats.totalTips),
        )
        UserStatRow(
            label = "Total Visits",
            value = UserStats.totalVisits.toString(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        UserStatRow(
            label = "Avg Bill",
            value = currencyFormatter.format(UserStats.averageBill),
        )
        UserStatRow(
            label = "Avg Tip",
            value = currencyFormatter.format(UserStats.averageTip),
        )
        UserStatRow(
            label = "Avg Tip Percent",
            value = String.format(
                Locale.getDefault(),
                "%.1f",
                UserStats.averageTipPercent
            ) + "%",
        )
        UserStatRow(
            label = "Avg Spend",
            value = currencyFormatter.format(UserStats.averageSpend),
        )
        UserStatWithIconRow(
            label = "Avg Party Size",
            value = String.format(
                Locale.getDefault(),
                "%.1f",
                UserStats.averagePartySize
            ),
            imageRes = R.drawable.person,
        )
    }
}

@Composable
fun UserStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun UserStatWithIconRow(
    label: String,
    value: String,
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(imageRes),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun DiningLogRecord(
    recordLabel: String,
    recordStat: String?,
    log: DiningLogData?,
    modifier: Modifier = Modifier,
    @DrawableRes recordStatIconId: Int? = null,
    secondaryStat: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (log != null && recordStat != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = recordLabel,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = log.date,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            DiningRecordCard(
                log = log,
                recordStat = recordStat,
                secondaryStat = secondaryStat,
                recordStatIconId = recordStatIconId,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = recordLabel,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DiningRecordCard(
    log: DiningLogData,
    recordStat: String,
    modifier: Modifier = Modifier,
    @DrawableRes recordStatIconId: Int? = null,
    secondaryStat: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 6.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.trophy),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = log.restaurantName,
                    style = MaterialTheme.typography.labelMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (recordStatIconId != null) {
                    Icon(
                        painter = painterResource(recordStatIconId),
                        contentDescription = null
                    )
                }
                Text(
                    text = recordStat,
                    style = MaterialTheme.typography.labelMedium
                )
                if (secondaryStat != null) {
                    Text(
                        text = secondaryStat,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
            if (log.restaurantDescription.isNotBlank()) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = log.restaurantDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            start = 4.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordCardPreview() {
    TipTrackerTheme {
        DiningRecordCard(
            recordStat = "8",
            log = DiningLogData(
                billAmount = 100.12,
                tipAmount = 2.01,
                tipPercent = 20.00,
                personCount = 2,
                totalAmount = 100.12,
                totalAmountPerPerson = 35.96,
                restaurantName = "Sesame Sea Asian Bistro",
                restaurantDescription = "Very nice food w/ good service. Some options were not halal though, so be aware. Alhamdulillah",
                date = "June 24, 2024",
                favorite = true,
            ),
            recordStatIconId = R.drawable.person
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    TipTrackerTheme {
        ProfileScreen(
            logViewModel = viewModel()
        )
    }
}