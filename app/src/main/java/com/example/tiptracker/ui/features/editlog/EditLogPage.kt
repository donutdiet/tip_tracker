package com.example.tiptracker.ui.features.editlog

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.components.CustomThumb
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.convertDateToMillis
import com.example.tiptracker.utils.convertMillisToDate
import com.example.tiptracker.utils.formatDateForDisplay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogPage(
    uiState: EditLogUiState,
    onAction: (EditLogAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember(uiState.date) {
        convertDateToMillis(uiState.date)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    var localRatingValue by remember { mutableFloatStateOf(uiState.rating.toFloat()) }
    LaunchedEffect(uiState.rating) {
        localRatingValue = uiState.rating.toFloat()
    }

    when {
        uiState.isLoading -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxSize()
            ) {
                Text(text = "Loading...")
            }
            return
        }

        uiState.isNotFound -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxSize()
            ) {
                Text(text = "Log not found.")
            }
            return
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp, 4.dp)
    ) {
        OutlinedTextField(
            value = uiState.bill,
            onValueChange = { onAction(EditLogAction.BillChanged(it)) },
            label = { Text("Bill Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.receipt_long),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.tipAmount,
            onValueChange = { onAction(EditLogAction.TipAmountChanged(it)) },
            label = { Text("Tip Amount") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.hand_meal),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.partySize,
            onValueChange = { onAction(EditLogAction.PartySizeChanged(it)) },
            label = { Text("Party Size") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.restaurantName,
            onValueChange = { onAction(EditLogAction.RestaurantNameChanged(it)) },
            label = { Text("Restaurant Name") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.storefront),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.review,
            onValueChange = { onAction(EditLogAction.ReviewChanged(it)) },
            label = { Text("Write a review") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = formatDateForDisplay(uiState.date),
            onValueChange = {},
            label = { Text("Date") },
            supportingText = { Text("Click the calendar icon to set the date") },
            readOnly = true,
            placeholder = { Text("Tap to select a date") },
            leadingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        painter = painterResource(R.drawable.event),
                        contentDescription = "Open date picker",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Rating", style = MaterialTheme.typography.bodyLarge)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "%.1f".format(localRatingValue),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Slider(
                value = localRatingValue,
                onValueChange = { localRatingValue = it },
                onValueChangeFinished = {
                    val rounded = (localRatingValue * 10f).roundToInt() / 10.0
                    onAction(EditLogAction.RatingChanged(rounded))
                },
                valueRange = 0f..10f,
                thumb = { CustomThumb(MaterialTheme.colorScheme.primary) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onAction(EditLogAction.Update) },
            enabled = !uiState.isUpdating,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isUpdating) "Updating..." else "Update")
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onAction(EditLogAction.DateChanged(convertMillisToDate(millis)))
                        }
                        showDatePicker = false
                        focusManager.clearFocus()
                    }
                ) {
                    Text("Ok")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        focusManager.clearFocus()
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditLogPagePreview() {
    TipTrackerTheme {
        EditLogPage(
            uiState = EditLogUiState(
                bill = "128.12",
                tipAmount = "25.62",
                partySize = "2",
                restaurantName = "The Pearl",
                review = "Great food and service.",
                date = "2026-03-21",
                rating = 8.6,
                isLoading = false,
                isNotFound = false,
                errorMessage = null,
                isUpdating = false
            ),
            onAction = {}
        )
    }
}