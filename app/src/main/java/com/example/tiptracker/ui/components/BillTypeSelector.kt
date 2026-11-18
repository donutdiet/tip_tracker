package com.example.tiptracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.ui.theme.TipTrackerTheme

/**
 * Split vs separate checks dropdown, sized to sit beside the party size field. Disabled when
 * the choice doesn't matter (a party of one) so the row keeps its layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillTypeSelector(
    selected: BillType,
    onSelect: (BillType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Bill", style = MaterialTheme.typography.titleSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            supportingText = supportingText,
            singleLine = true,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            BillType.entries.forEach { billType ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(billType.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = billType.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(billType)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

private val BillType.label: String
    get() = when (this) {
        BillType.SPLIT -> "Split evenly"
        BillType.SEPARATE -> "Separate checks"
    }

private val BillType.description: String
    get() = when (this) {
        BillType.SPLIT -> "One bill divided evenly across the party"
        BillType.SEPARATE -> "Everyone paid their own. Log only your check"
    }

@Preview(showBackground = true)
@Composable
fun BillTypeSelectorPreview() {
    TipTrackerTheme {
        BillTypeSelector(selected = BillType.SEPARATE, onSelect = {})
    }
}
