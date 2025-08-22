package com.example.tiptracker.ui_old

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tiptracker.ui.theme.TipTrackerTheme

@Composable
fun RankingsScreen(
    modifier: Modifier = Modifier
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Coming soon... ")
        }
    }
}

@Preview
@Composable
fun RankingsScreenPreview() {
    TipTrackerTheme {
        RankingsScreen()
    }
}