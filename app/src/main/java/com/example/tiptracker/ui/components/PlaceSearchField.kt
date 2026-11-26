package com.example.tiptracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.ui.placesearch.SelectedPlace
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.rememberLocationPermissionPrompt

/**
 * Restaurant name input with suggestions: the user's past restaurants ([history]) first, then
 * Google Maps places. Whatever is in the field is the name that gets saved; picking a Google
 * suggestion fills in its name and links the log to that place ([selectedPlace]), whose
 * official name and address are shown below for reference. Picking a past restaurant fills in
 * the name used last time and links its place, if it has one. Typing never unlinks the place;
 * the Unlink button does. Offline (or if a search fails) only past restaurants are suggested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchField(
    query: String,
    predictions: List<PlacePrediction>,
    history: List<RestaurantHistory>,
    // The user's past visits to places in [predictions], by place ID
    visitedPlaces: Map<String, RestaurantHistory>,
    selectedPlace: SelectedPlace?,
    isOffline: Boolean,
    searchFailed: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlacePrediction) -> Unit,
    onHistorySelected: (RestaurantHistory) -> Unit,
    onClearPlace: () -> Unit,
    modifier: Modifier = Modifier,
    // Called when the field gains focus with no place linked; may search the current text
    onFocused: () -> Unit = {},
    // Called when the user taps "Show address" on a place whose details weren't loaded
    onShowAddress: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    // Holds the cursor as well as the text, so that when the text is set from outside (e.g.
    // filled with a picked place's name, or a log loading) the cursor can go to the end
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        // Text the user typed already matches; anything else came from outside
        if (query != fieldValue.text) fieldValue = TextFieldValue(query, TextRange(query.length))
    }
    // No suggestions while a place is linked; Unlink first to pick another
    val showSuggestions = expanded && selectedPlace == null &&
        (predictions.isNotEmpty() || history.isNotEmpty())
    val promptForLocation = rememberLocationPermissionPrompt()

    // Search status only matters while the user can still pick a place
    val searching = selectedPlace == null
    val supportingText = when {
        searching && searchFailed -> "Couldn't load suggestions. You can still type the name."
        searching && isOffline -> "Google suggestions aren't available offline."
        else -> null
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = showSuggestions,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = {
                    val textChanged = it.text != fieldValue.text
                    fieldValue = it
                    // Cursor moves and selection changes aren't edits
                    if (textChanged) {
                        expanded = true
                        onQueryChange(it.text)
                    }
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
                if (history.isNotEmpty()) {
                    SuggestionsHeader {
                        Text(
                            text = "From your logs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                history.forEach { past ->
                    DropdownMenuItem(
                        text = { HistorySuggestion(past) },
                        onClick = {
                            expanded = false
                            onHistorySelected(past)
                        }
                    )
                }
                if (predictions.isNotEmpty()) {
                    if (history.isNotEmpty()) HorizontalDivider()
                    // Required by the Places policies, as predictions aren't shown on a Google
                    // map. As the header, it labels exactly the rows that came from Google.
                    SuggestionsHeader {
                        GoogleMapsAttribution(background = MaterialTheme.colorScheme.surfaceContainerLow)
                    }
                }
                predictions.forEach { prediction ->
                    val visited = visitedPlaces[prediction.placeId]
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = SUGGESTION_VERTICAL_PADDING)) {
                                Text(
                                    // A place the user has logged shows the name they use for it
                                    text = visited?.name ?: prediction.name,
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
                                visited?.let {
                                    Text(
                                        text = visitsText(it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onPlaceSelected(prediction)
                        }
                    )
                }
            }
        }

        selectedPlace?.let { place ->
            SelectedPlaceInfo(
                place = place,
                onShowAddress = onShowAddress,
                onClear = {
                    // Open the list for the fresh results of the current text
                    expanded = true
                    onClearPlace()
                }
            )
        }
    }
}

/** Labels a section of the suggestions list, lined up with the rows' text. */
@Composable
private fun SuggestionsHeader(content: @Composable () -> Unit) {
    // The rows' own horizontal padding, so the header starts where their text does
    val rowPadding = MenuDefaults.DropdownMenuItemContentPadding
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = Modifier.padding(
            start = rowPadding.calculateStartPadding(layoutDirection),
            end = rowPadding.calculateEndPadding(layoutDirection),
            top = 8.dp,
            bottom = 4.dp
        )
    ) {
        content()
    }
}

/** A past restaurant: its name, then how often and when it was last visited. */
@Composable
private fun HistorySuggestion(history: RestaurantHistory) {
    Column(modifier = Modifier.padding(vertical = SUGGESTION_VERTICAL_PADDING)) {
        Text(
            text = history.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = visitsText(history),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun visitsText(history: RestaurantHistory): String {
    val lastVisited = formatDateForDisplay(history.lastVisited)
    return if (history.visitCount == 1) {
        "Visited $lastVisited"
    } else {
        "Visited ${history.visitCount}× · last $lastVisited"
    }
}

/**
 * The linked place in a small tinted box under the field, with a button to unlink it. A place
 * picked from suggestions shows its name and address right away; one linked when the screen
 * opened shows "Show address" until tapped, since that lookup is billed. Google's attribution
 * is shown whenever Google details are.
 */
@Composable
private fun SelectedPlaceInfo(place: SelectedPlace, onShowAddress: () -> Unit, onClear: () -> Unit) {
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    // Collapsed it's a single line, so center everything and let the Unlink button set the
    // height; expanded, top-align so Unlink stays beside the first line
    val collapsed = !place.hasDetails
    // Expanded, the icon and Unlink are centered on the first text line: the bold name for a
    // picked suggestion, or the smaller address for a place looked up on tap
    val nameStyle = MaterialTheme.typography.bodyMedium
    val addressStyle = MaterialTheme.typography.bodySmall
    val firstLineHeight = with(LocalDensity.current) {
        (if (place.name != null) nameStyle else addressStyle).lineHeight.toDp()
    }
    val iconOffset = if (collapsed) 0.dp else (firstLineHeight - ICON_SIZE) / 2
    val clearOffset = if (collapsed) 0.dp else (firstLineHeight - CLEAR_MIN_HEIGHT) / 2
    Surface(
        color = background,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = if (collapsed) Alignment.CenterVertically else Alignment.Top,
            modifier = Modifier.padding(
                start = 12.dp,
                top = if (collapsed) 4.dp else 10.dp,
                bottom = if (collapsed) 4.dp else 10.dp,
                end = 4.dp
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.location),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .offset(y = iconOffset)
                    .size(ICON_SIZE)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (place.hasDetails) {
                    place.name?.let {
                        Text(
                            text = it,
                            style = nameStyle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    place.address?.let {
                        // Without a name (looked up on tap), the address is the top line, so it
                        // takes the name's emphasis
                        val isTopLine = place.name == null
                        Text(
                            text = it,
                            style = addressStyle,
                            fontWeight = if (isTopLine) FontWeight.SemiBold else null,
                            color = if (isTopLine) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    place.attributions.forEach {
                        Text(
                            text = AnnotatedString.fromHtml(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GoogleMapsAttribution(
                        background = background,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = when {
                            place.isLoading -> "Loading address…"
                            place.lookupFailed -> "Couldn't load the address. Tap to try again."
                            else -> "Show address"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (place.isLoading) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(
                                enabled = !place.isLoading,
                                role = Role.Button,
                                onClick = onShowAddress
                            )
                    )
                }
            }

            // Material buttons silently reserve a 48dp touch target, which would make this one
            // taller than it looks and throw off both the centering math and the box height.
            // Keep it at the 32dp it draws.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .heightIn(min = CLEAR_MIN_HEIGHT)
                        // Shift up so its label centers on the first text line
                        .offset(y = clearOffset)
                        .semantics { contentDescription = "Unlink Google Maps place" }
                ) {
                    // Hidden from accessibility so only the fuller description above is read
                    Text(
                        "Unlink",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            }
        }
    }
}

private val ICON_SIZE = 18.dp
// Menu items center their content in a fixed-height row with no vertical padding, so a
// three-line suggestion would touch its edges
private val SUGGESTION_VERTICAL_PADDING = 4.dp
private val CLEAR_MIN_HEIGHT = 32.dp

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "Pearl",
            predictions = emptyList(),
            history = emptyList(),
            visitedPlaces = emptyMap(),
            selectedPlace = SelectedPlace(
                placeId = "id",
                name = "The Pearl",
                address = "123 Main St, Portland, OR"
            ),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
            onHistorySelected = {},
            onClearPlace = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldAddressOnlyPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "Pearl",
            predictions = emptyList(),
            history = emptyList(),
            visitedPlaces = emptyMap(),
            selectedPlace = SelectedPlace(placeId = "id", address = "123 Main St, Portland, OR"),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
            onHistorySelected = {},
            onClearPlace = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldAddressHiddenPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "Pearl",
            predictions = emptyList(),
            history = emptyList(),
            visitedPlaces = emptyMap(),
            selectedPlace = SelectedPlace(placeId = "id"),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
            onHistorySelected = {},
            onClearPlace = {}
        )
    }
}
