package com.example.tiptracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.ui.theme.TipTrackerTheme

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

@Preview(showBackground = true)
@Composable
fun AppButtonPreview() {
    TipTrackerTheme {
        Column {
            AppButton(
                text = "Button",
                enabled = true,
                onClick = {}
            )
            AppButton(
                text = "Disabled",
                enabled = false,
                onClick = {}
            )
        }
    }
}