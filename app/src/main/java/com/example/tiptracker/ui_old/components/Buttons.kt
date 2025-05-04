package com.example.tiptracker.ui_old.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.ui_old.theme.TipTrackerTheme

@Composable
fun AppButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        shape = RoundedCornerShape(4.dp),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
    }
}

@Composable
fun AppButtonOutlined(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        shape = RoundedCornerShape(4.dp),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
    }
}

@Preview
@Composable
fun AppButtonPreview() {
    TipTrackerTheme {
        AppButtonOutlined(
            text = "Button",
            enabled = false,
            onClick = {},
        )
    }
}