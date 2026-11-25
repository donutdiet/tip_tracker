package com.example.tiptracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.model.NameCount
import com.example.tiptracker.ui.theme.TipTrackerTheme

/**
 * Asks which name a place's logs should share, when saving a log whose name differs from the
 * place's other logs. [choices] lists the name being saved first (see placeNameChoices).
 */
data class PlaceNamePrompt(
    val choices: List<NameCount>,
    // Starts selected: the name being saved if the user changed it, otherwise the most used
    val initial: String
)

/**
 * A centered title and message, the names as selectable cards, a full-width button to use the
 * chosen name and a quieter Cancel under it.
 *
 * @param onSave called with the chosen name, which the log being saved and the place's other
 *   logs all take.
 * @param onCancel closes the dialog without saving.
 */
@Composable
fun PlaceNameDialog(
    prompt: PlaceNamePrompt,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var selected by remember(prompt) { mutableStateOf(prompt.initial) }
    val thisLogsName = prompt.choices.first().name
    val totalLogs = prompt.choices.sumOf { it.count }
    val colors = MaterialTheme.colorScheme
    val buttonShape = RoundedCornerShape(4.dp)
    val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Choose one name",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                // Without an icon the dialog start-aligns the title slot, so fill it to center
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = "You've logged this location under ${prompt.choices.size} different " +
                        "names. Pick the one to use for all $totalLogs logs here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup()
                ) {
                    prompt.choices.forEach { choice ->
                        NameChoiceCard(
                            choice = choice,
                            isThisLog = choice.name == thisLogsName,
                            selected = choice.name == selected,
                            onSelect = { selected = choice.name }
                        )
                    }
                }
            }
        },
        // The choice is the main action, so it gets a full-width button; Cancel is a quiet text
        // button under it
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onSave(selected) },
                    shape = buttonShape,
                    contentPadding = buttonPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                ) {
                    Text("Use this name", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                // Material reserves a 48dp touch target, which left a tall gap under the main
                // button; 40dp is still comfortable to tap
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

/** One name as a card: highlighted with the primary color when selected. */
@Composable
private fun NameChoiceCard(
    choice: NameCount,
    isThisLog: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        label = "nameChoiceContainer"
    )
    val border by animateColorAsState(
        if (selected) colors.primary else colors.outlineVariant,
        label = "nameChoiceBorder"
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        ) {
            // The card handles the click, so the whole card is the target
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(if (choice.count == 1) "1 log" else "${choice.count} logs")
                        if (isThisLog) append(" · this log")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlaceNameDialogPreview() {
    TipTrackerTheme {
        PlaceNameDialog(
            prompt = PlaceNamePrompt(
                choices = listOf(NameCount("BWW", 1), NameCount("Buffalo Wild Wings", 5)),
                initial = "BWW"
            ),
            onSave = {},
            onCancel = {}
        )
    }
}
