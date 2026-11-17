package com.example.tiptracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Required by the Places policies whenever Places content is shown outside a Google map.
 * Text form ("Google Maps") per Google's styling rules: unmodified and untranslated, one line,
 * 12-16sp, weight 400, white or #5E5E5E/#1F1F1F with at least 4.5:1 contrast.
 *
 * @param background the color the text sits on, used to pick a legible text color.
 */
@Composable
fun GoogleMapsAttribution(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background
) {
    Text(
        text = "Google Maps",
        color = if (background.luminance() < 0.5f) Color.White else Color(0xFF5E5E5E),
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}
