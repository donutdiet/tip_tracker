package com.example.tiptracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.ui.placesearch.SelectedPlace
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.rememberLocationPermissionPrompt

/**
 * Restaurant name input with Google Maps suggestions. Whatever the user types is the name that
 * gets saved; picking a suggestion only links the log to that place ([selectedPlace]), whose
 * official name and address are shown below for reference. Typing never unlinks the place;
 * the Clear button does. Offline (or if a search fails) it is just a free-text field.
 *
 * @param address a free-text address the user entered in an older version of the app, shown
 *   when no place is linked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchField(
    query: String,
    predictions: List<PlacePrediction>,
    selectedPlace: SelectedPlace?,
    isOffline: Boolean,
    searchFailed: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlacePrediction) -> Unit,
    onClearPlace: () -> Unit,
    modifier: Modifier = Modifier,
    address: String = "",
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    // No suggestions while a place is linked; Clear first to pick another
    val showSuggestions = expanded && selectedPlace == null && predictions.isNotEmpty()
    val promptForLocation = rememberLocationPermissionPrompt()

    // Search status only matters while the user can still pick a place
    val searching = selectedPlace == null
    val supportingText = when {
        searching && searchFailed -> "Couldn't load suggestions. You can still type the name."
        searching && isOffline -> "Restaurant search isn't available offline. Enter the name manually."
        address.isNotEmpty() -> address
        else -> null
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = showSuggestions,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    expanded = true
                    onQueryChange(it)
                },
                label = { Text("Restaurant Name", style = MaterialTheme.typography.titleSmall) },
                supportingText = supportingText?.let { text ->
                    { Text(text, style = MaterialTheme.typography.bodySmall) }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = imeAction
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.storefront),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
                    .onFocusChanged { focus ->
                        if (focus.isFocused) {
                            if (!isOffline) promptForLocation()
                            // Reopen any suggestions from earlier typing
                            if (selectedPlace == null) expanded = true
                        } else {
                            expanded = false
                        }
                    }
            )

            ExposedDropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { expanded = false }
            ) {
                predictions.forEach { prediction ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = prediction.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = prediction.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onPlaceSelected(prediction)
                        }
                    )
                }
                // Required by the Places policies: predictions aren't shown on a Google map
                HorizontalDivider()
                GoogleMapsAttribution(
                    background = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .wrapContentWidth(Alignment.End)
                )
            }
        }

        selectedPlace?.let { place ->
            SelectedPlaceInfo(
                place = place,
                onClear = {
                    // Open the list for the fresh results of the current text
                    expanded = true
                    onClearPlace()
                }
            )
        }
    }
}

/** The linked place's Google details, with the attribution Google requires and a Clear button. */
@Composable
private fun SelectedPlaceInfo(place: SelectedPlace, onClear: () -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
        when {
            place.name != null || place.address != null -> {
                place.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                place.address?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                place.attributions.forEach {
                    Text(
                        text = AnnotatedString.fromHtml(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> Text(
                text = if (place.lookupFailed) {
                    "Linked to a Google Maps place. Its details couldn't be loaded."
                } else {
                    "Loading place details…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            // Styled as a text link rather than a full button to keep the row compact
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(vertical = 2.dp)
            )
            GoogleMapsAttribution()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "Pearl",
            predictions = emptyList(),
            selectedPlace = SelectedPlace(
                placeId = "id",
                name = "The Pearl",
                address = "123 Main St, Portland, OR"
            ),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
            onClearPlace = {}
        )
    }
}
