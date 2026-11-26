package com.example.tiptracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.data.repository.PlaceDetails

/**
 * A linked place's address. Looking it up is billed, so it's fetched only once the user taps
 * "Show address", then shown with the attributions Google requires. [failed] offers a retry.
 */
@Composable
fun PlaceAddress(
    details: PlaceDetails?,
    isLoading: Boolean,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
    failed: Boolean = false
) {
    val address = details?.address
    if (address == null) {
        val color = if (isLoading) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = !isLoading, role = Role.Button, onClick = onShow)
                .padding(vertical = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.location),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    isLoading -> "Loading address…"
                    failed -> "Couldn't load the address. Try again"
                    else -> "Show address"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
        return
    }

    val iconSize = 16.dp
    val iconGap = 4.dp
    // Lines under the address start where the address text does, not under the icon
    val textIndent = Modifier.padding(start = iconSize + iconGap)

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.location),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(iconGap))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        details.attributions.forEach {
            Text(
                // May contain HTML links
                text = AnnotatedString.fromHtml(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = textIndent
            )
        }
        GoogleMapsAttribution(modifier = textIndent)
    }
}
