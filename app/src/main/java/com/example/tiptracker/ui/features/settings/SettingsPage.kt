package com.example.tiptracker.ui.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.example.tiptracker.ui.components.CustomThumb
import com.example.tiptracker.ui.components.LabeledSwitch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use local values to display on UI as user scrolls; only write to DataStore on slider release
    var localTipPreset1Value by remember { mutableFloatStateOf(uiState.tipPreset1Percent.toFloat()) }
    var localTipPreset2Value by remember { mutableFloatStateOf(uiState.tipPreset2Percent.toFloat()) }

    LaunchedEffect(uiState.tipPreset1Percent, uiState.tipPreset2Percent) {
        localTipPreset1Value = uiState.tipPreset1Percent.toFloat()
        localTipPreset2Value = uiState.tipPreset2Percent.toFloat()
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 24.dp)
    ) {
        LabeledSwitch(
            label = "Dark Mode",
            checked = uiState.darkMode,
            onToggle = { onAction(SettingsAction.setDarkMode(!uiState.darkMode)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tip Percent Preset 1")
            Text("${localTipPreset1Value.roundToInt()}%")
        }
        Slider(
            value = localTipPreset1Value,
            onValueChange = { localTipPreset1Value = it },
            onValueChangeFinished = { onAction(SettingsAction.setTipPreset1Percent(localTipPreset1Value.roundToInt())) },
            valueRange = 0f..50f,
            thumb = { CustomThumb(MaterialTheme.colorScheme.primary) }
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tip Percent Preset 2")
            Text("${localTipPreset2Value.roundToInt()}%")
        }
        Slider(
            value = localTipPreset2Value,
            onValueChange = { localTipPreset2Value = it },
            onValueChangeFinished = { onAction(SettingsAction.setTipPreset2Percent(localTipPreset2Value.roundToInt())) },
            valueRange = 0f..50f,
            thumb = { CustomThumb(MaterialTheme.colorScheme.primary) }
        )
    }
}