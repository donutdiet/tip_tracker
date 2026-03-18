package com.example.tiptracker.ui.tabs.home.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.tiptracker.ui.tabs.home.HomeAction
import com.example.tiptracker.ui.tabs.home.HomeUiState
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.convertDateToMillis
import com.example.tiptracker.utils.convertMillisToDate
import com.example.tiptracker.utils.formatDateForDisplay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPage(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var showDatePicker by remember { mutableStateOf(false) }

    val initialDateMillis = remember(uiState.date) {
        convertDateToMillis(uiState.date)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    // Use local rating value for smooth slider interaction; only update state on slider release
    var localRatingValue by remember { mutableFloatStateOf(uiState.rating.toFloat()) }

    LaunchedEffect(uiState.rating) {
        localRatingValue = uiState.rating.toFloat()
    }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp, 4.dp)
    ) {
        OutlinedTextField(
            value = uiState.restaurantName,
            onValueChange = { onAction(HomeAction.onRestaurantNameChange(it)) },
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
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.review,
            onValueChange = { onAction(HomeAction.onReviewChange(it)) },
            label = { Text("Review") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            minLines = 5,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formatDateForDisplay(uiState.date),
            onValueChange = {},
            label = { Text("Date") },
            supportingText = { Text("Click the calendar icon to set the date") },
            readOnly = true,
            placeholder = { Text("Tap to select a date") },
            leadingIcon = {
                IconButton(
                    onClick = { showDatePicker = true }
                ) {
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
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rating")
            Text("%.1f".format(localRatingValue))
        }
        Slider(
            value = localRatingValue,
            onValueChange = { localRatingValue = it },
            onValueChangeFinished = {
                val rounded = (localRatingValue * 10f).roundToInt() / 10.0
                onAction(HomeAction.onRatingChange(rounded))
            },
            valueRange = 0f..10f,
            thumb = { CustomThumb(MaterialTheme.colorScheme.primary) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onBack() },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(2f)
            ) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onAction(HomeAction.saveLog) },
                shape = RoundedCornerShape(4.dp),
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(3f)
            ) {
                Text(text = if (uiState.isSaving) "Saving..." else "Save")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onAction(HomeAction.onDateChange(convertMillisToDate(millis)))
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
fun ReviewPagePreview() {
    TipTrackerTheme {
        ReviewPage(
            uiState = HomeUiState(
                billAmount = "154.28",
                tipPreset1 = true,
                tipPreset2 = false,
                tipPercent = "15",
                partySize = "2",
                roundUpTip = true,
                roundUpTotal = false,
                restaurantName = "The Pearl",
                review = "Oysters were crazy",
                date = "2026-03-21",
                rating = 8.7,
                isSaving = false,
                tipPreset1Percent = 15,
                tipPreset2Percent = 20
            ),
            onAction = {},
            onBack = {}
        )
    }
}
