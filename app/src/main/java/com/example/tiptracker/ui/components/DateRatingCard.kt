package com.example.tiptracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.convertDateToMillis
import com.example.tiptracker.utils.convertMillisToDate
import com.example.tiptracker.utils.formatDateForDisplay
import kotlin.math.roundToInt

/**
 * The log's date and rating as two matching rows in one outlined box, styled to sit with the
 * form's text fields. Tapping the date row opens the date picker.
 *
 * @param date in yyyy-MM-dd form; [onDateChange] receives the same form.
 * @param onRatingChange called once the slider is released, rounded to one decimal.
 */
@Composable
fun DateRatingCard(
    date: String,
    onDateChange: (String) -> Unit,
    rating: Double,
    onRatingChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column {
            DateRow(date = date, onDateChange = onDateChange)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RatingRow(rating = rating, onRatingChange = onRatingChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRow(date: String, onDateChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Change date",
                onClick = {
                    focusManager.clearFocus()
                    showDatePicker = true
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        FieldLabel("Date", modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.takeIf { it.isNotBlank() }?.let(::formatDateForDisplay)
                    ?: "Select a date",
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                painter = painterResource(R.drawable.event),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDatePicker) {
        // Created with the dialog so it always opens on the current date, including one
        // loaded after this composable first appeared (the edit screen)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = convertDateToMillis(date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(convertMillisToDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Ok", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingRow(rating: Double, onRatingChange: (Double) -> Unit) {
    // Local value for smooth dragging; the form only updates when the slider is released
    var localRating by remember { mutableFloatStateOf(rating.toFloat()) }
    LaunchedEffect(rating) { localRating = rating.toFloat() }

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("Rating", modifier = Modifier.weight(1f))
            Text(
                text = buildAnnotatedString {
                    append("%.1f".format(localRating))
                    withStyle(
                        MaterialTheme.typography.bodySmall.toSpanStyle()
                            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        append(" / 10")
                    }
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Slider(
            value = localRating,
            onValueChange = { localRating = it },
            onValueChangeFinished = {
                onRatingChange((localRating * 10f).roundToInt() / 10.0)
            },
            valueRange = 0f..10f,
            thumb = { CustomThumb(MaterialTheme.colorScheme.primary) }
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DateRatingCardPreview() {
    TipTrackerTheme {
        DateRatingCard(
            date = "2026-03-21",
            onDateChange = {},
            rating = 8.7,
            onRatingChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
