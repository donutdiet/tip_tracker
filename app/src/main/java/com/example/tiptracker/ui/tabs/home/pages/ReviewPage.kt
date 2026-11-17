package com.example.tiptracker.ui.tabs.home.pages

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.ui.components.DateRatingCard
import com.example.tiptracker.ui.components.PlaceSearchField
import com.example.tiptracker.ui.tabs.home.HomeAction
import com.example.tiptracker.ui.tabs.home.HomeUiState
import com.example.tiptracker.ui.theme.TipTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPage(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current


    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp, 4.dp)
    ) {
        PlaceSearchField(
            query = uiState.restaurantName,
            predictions = uiState.predictions,
            selectedPlace = uiState.selectedPlace,
            isOffline = uiState.isOffline,
            searchFailed = uiState.searchFailed,
            onQueryChange = { onAction(HomeAction.onRestaurantQueryChange(it)) },
            onPlaceSelected = { onAction(HomeAction.onPlaceSelected(it)) },
            onClearPlace = { onAction(HomeAction.onClearPlace) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.review,
            onValueChange = { onAction(HomeAction.onReviewChange(it)) },
            label = {
                Text("Write a review", style = MaterialTheme.typography.titleSmall)
            },
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
        Spacer(modifier = Modifier.height(8.dp))

        DateRatingCard(
            date = uiState.date,
            onDateChange = { onAction(HomeAction.onDateChange(it)) },
            rating = uiState.rating,
            onRatingChange = { onAction(HomeAction.onRatingChange(it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onBack() },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(2f)
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onAction(HomeAction.saveLog) },
                shape = RoundedCornerShape(4.dp),
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    text = if (uiState.isSaving) "Saving..." else "Save",
                    style = MaterialTheme.typography.labelLarge
                )
            }
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