package com.example.tiptracker.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
                        // No bottom padding: the menu already adds 8dp below its last item
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                        .wrapContentWidth(Alignment.End)
                )
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

/**
 * The linked place in a small tinted box under the field, with a button to unlink it. A place
 * picked from suggestions shows its name and address right away; one linked when the screen
 * opened shows "Show address" until tapped, since that lookup is billed. Google's attribution
 * is shown whenever Google details are.
 */
@Composable
private fun SelectedPlaceInfo(place: SelectedPlace, onShowAddress: () -> Unit, onClear: () -> Unit) {
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    // Collapsed it's a single line, so center everything and let the Clear button set the
    // height; expanded, top-align so Clear stays beside the first line
    val collapsed = !place.hasDetails
    // Expanded, the icon and Clear are centered on the first text line: the bold name for a
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
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private val ICON_SIZE = 18.dp
private val CLEAR_MIN_HEIGHT = 32.dp

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

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldAddressOnlyPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "Pearl",
            predictions = emptyList(),
            selectedPlace = SelectedPlace(placeId = "id", address = "123 Main St, Portland, OR"),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
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
            selectedPlace = SelectedPlace(placeId = "id"),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {},
            onClearPlace = {}
        )
    }
}
