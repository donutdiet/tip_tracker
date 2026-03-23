package com.example.tiptracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomThumb(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = 24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}