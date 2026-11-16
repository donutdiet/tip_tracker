package com.example.tiptracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.utils.rememberLocationPermissionPrompt

/**
 * Combined restaurant name + address input. Online, it suggests nearby places as the user
 * types; offline (or if a search fails) it is just a free-text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchField(
    query: String,
    address: String,
    predictions: List<PlacePrediction>,
    isOffline: Boolean,
    searchFailed: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlacePrediction) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    val promptForLocation = rememberLocationPermissionPrompt()

    val supportingText = when {
        searchFailed -> "Couldn't load suggestions. You can still type the name."
        isOffline -> "Restaurant search isn't available offline. Enter the name manually."
        address.isNotEmpty() -> address
        else -> null
    }

    ExposedDropdownMenuBox(
        expanded = expanded && predictions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
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
                    if (focus.isFocused && !isOffline) promptForLocation()
                    if (!focus.isFocused) expanded = false
                }
        )

        ExposedDropdownMenu(
            expanded = expanded && predictions.isNotEmpty(),
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchFieldPreview() {
    TipTrackerTheme {
        PlaceSearchField(
            query = "The Pearl",
            address = "123 Main St, Portland, OR",
            predictions = emptyList(),
            isOffline = false,
            searchFailed = false,
            onQueryChange = {},
            onPlaceSelected = {}
        )
    }
}
