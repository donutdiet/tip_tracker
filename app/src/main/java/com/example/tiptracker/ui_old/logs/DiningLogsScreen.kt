package com.example.tiptracker.ui_old.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui_old.DiningLogData
import com.example.tiptracker.ui_old.UserStats
import com.example.tiptracker.ui_old.addentry.LogViewModel
import com.example.tiptracker.ui.theme.TipTrackerTheme
import java.text.NumberFormat

private enum class HorizontalDragValue { Settled, StartToEnd, EndToStart }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiningLogsScreen(
    modifier: Modifier = Modifier,
    diningLogs: List<DiningLogData>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    logViewModel: LogViewModel,
    onFavoriteButtonClicked: (Int) -> Unit,
    onEditButtonClicked: (Int) -> Unit,
    onDeleteButtonClicked: (Int) -> Unit
) {
    if (logViewModel.diningLogs.isNotEmpty()) {
        Column(modifier = modifier) {
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Dining Logs",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = "A record of all your dining experiences",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                itemsIndexed(diningLogs) { index, log ->
//                    var boxSize by remember { mutableFloatStateOf(0F) }
//                    val scope = rememberCoroutineScope()
//                    val anchors = DraggableAnchors {
//                        HorizontalDragValue.Settled at 0f
//                        HorizontalDragValue.StartToEnd at boxSize / 5
//                        HorizontalDragValue.EndToStart at -boxSize / 4
//                    }
//                    val state = remember {
//                        AnchoredDraggableState(
//                            initialValue = HorizontalDragValue.Settled,
//                            positionalThreshold = { distance -> distance * 0.3f },
//                            velocityThreshold = { 0.3f },
//                            animationSpec = tween()
//                        )
//                    }
//                    SideEffect { state.updateAnchors(anchors) }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .animateContentSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(0.20f)
                                .align(Alignment.CenterStart),
//                                .clickable {
//                                    scope.launch { state.animateTo(HorizontalDragValue.Settled) }
//                                    onFavoriteButtonClicked(index)
//                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = if (diningLogs[index].favorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth(0.24f)
                                .align(Alignment.CenterEnd),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable { onEditButtonClicked(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
//                                    .clickable {
//                                        scope.launch { state.animateTo(HorizontalDragValue.Settled) }
//                                        onDeleteButtonClicked(index)
//                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
//                                .graphicsLayer { boxSize = size.width }
//                                .offset {
//                                    IntOffset(
//                                        x = state
//                                            .requireOffset()
//                                            .roundToInt(), y = 0
//                                    )
//                                }
//                                .fillMaxWidth()
//                                .anchoredDraggable(state, Orientation.Horizontal)
                        ) {
                            DiningEntry(
                                log = log,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "Swipe right on a log to favorite",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Swipe left to edit/delete",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nothing logged",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun DiningEntry(
    log: DiningLogData,
    modifier: Modifier = Modifier,
) {
    val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance()
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.clickable { expanded = !expanded },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = log.restaurantName,
                            style = MaterialTheme.typography.labelMedium,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyFormatter.format(log.totalAmount),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.widthIn(min = 64.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.date,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp, end = 1.dp)
                        )
                        Text(
                            text = log.personCount.toString(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (log.favorite) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (
                            log.id == UserStats.highestSpendLogId ||
                            log.id == UserStats.highestTipPercentLogId ||
                            log.id == UserStats.largestPartySizeLogId
                        ) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(R.drawable.trophy_filled),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "Tip:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyFormatter.format(log.tipAmount),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.widthIn(min = 64.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
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
fun DiningEntryPreview() {
    TipTrackerTheme {
        DiningEntry(
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
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DiningEntryPreviewDarkTheme() {
    TipTrackerTheme(darkTheme = true) {
        DiningEntry(
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
            )
        )
    }
}