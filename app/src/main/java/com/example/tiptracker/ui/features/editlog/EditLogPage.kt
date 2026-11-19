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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.ui.components.BillTypeSelector
import com.example.tiptracker.ui.components.DateRatingCard
import com.example.tiptracker.ui.components.PlaceSearchField
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.titleSmallMono
import com.example.tiptracker.utils.formatTipPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogPage(
    uiState: EditLogUiState,
    onAction: (EditLogAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current


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

        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Tip Percent", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${formatTipPercent(uiState.tipPercent)}%", style = MaterialTheme.typography.titleSmallMono)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Total", style = MaterialTheme.typography.bodyMedium)
                Text(text = "$${uiState.total}", style = MaterialTheme.typography.titleSmallMono)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.partySize,
                onValueChange = { onAction(EditLogAction.PartySizeChanged(it)) },
                // "Party Size" wraps to two lines at this width and misaligns the row
                label = { Text("Party") },
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
                modifier = Modifier.weight(2f)
            )
            BillTypeSelector(
                selected = uiState.billType,
                onSelect = { onAction(EditLogAction.BillTypeChanged(it)) },
                enabled = uiState.billTypeApplies,
                supportingText = if (uiState.billTypeApplies && uiState.billType == BillType.SEPARATE) {
                    { Text("Bill and tip are only yours") }
                } else null,
                modifier = Modifier.weight(3f)
            )
        }

        PlaceSearchField(
            query = uiState.restaurantName,
            address = uiState.address,
            predictions = uiState.predictions,
            selectedPlace = uiState.selectedPlace,
            isOffline = uiState.isOffline,
            searchFailed = uiState.searchFailed,
            onQueryChange = { onAction(EditLogAction.RestaurantQueryChanged(it)) },
            onPlaceSelected = { onAction(EditLogAction.PlaceSelected(it)) },
            onClearPlace = { onAction(EditLogAction.ClearPlace) },
            onFocused = { onAction(EditLogAction.RestaurantFieldFocused) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.review,
            onValueChange = { onAction(EditLogAction.ReviewChanged(it)) },
            label = { Text("Edit your review") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            minLines = 8,
            maxLines = 16,
            modifier = Modifier.fillMaxWidth()
        )

        DateRatingCard(
            date = uiState.date,
            onDateChange = { onAction(EditLogAction.DateChanged(it)) },
            rating = uiState.rating,
            onRatingChange = { onAction(EditLogAction.RatingChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

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
                address = "address",
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