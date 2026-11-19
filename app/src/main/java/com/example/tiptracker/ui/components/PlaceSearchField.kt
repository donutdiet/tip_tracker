package com.example.tiptracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
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
    // Called when the field gains focus with no place linked; may search the current text
    onFocused: () -> Unit = {},
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
        searching && isOffline -> "Restaurant search isn't available offline."
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
                            // Reopen suggestions from earlier typing, fetching them again
                            // if they were cleared (e.g. while offline)
                            if (selectedPlace == null) {
                                expanded = true
                                onFocused()
                            }
                        } else {
                            expanded = false
                        }
                    }
            )

            ExposedDropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                    background = MaterialTheme.colorScheme.surfaceContainerLow,
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

/**
 * The linked place's Google details in a small tinted box under the field, with the
 * attribution Google requires and a button to unlink it.
 */
@Composable
private fun SelectedPlaceInfo(place: SelectedPlace, onClear: () -> Unit) {
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        color = background,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.location),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (place.name != null || place.address != null) {
                    place.name?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (place.lookupFailed) {
                            "Linked to a Google Maps place. Its details couldn't be loaded."
                        } else {
                            "Loading place details…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GoogleMapsAttribution(
                    background = background,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    // The 32dp button centers its label 6dp below the name's first line
                    // (20sp line height); shift it up so the two line up
                    .offset(y = (-6).dp)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
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
