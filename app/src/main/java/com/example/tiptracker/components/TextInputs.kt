package com.example.tiptracker.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import java.time.LocalDate

@Composable
fun AppTextField(
    @DrawableRes leadingIcon: Int?,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        leadingIcon = leadingIcon?.let { id ->
            {
                Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun AppTextFieldDone(
    @DrawableRes leadingIcon: Int?,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        leadingIcon = leadingIcon?.let { id ->
            {
                Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() } // removes focus when done
        ),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun AppMultiLineTextField(
    @DrawableRes leadingIcon: Int?,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        leadingIcon = leadingIcon?.let { id ->
            {
                Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        maxLines = 12,
        modifier = modifier
    )
}

@Composable
fun AppMultiLineTextFieldDone(
    @DrawableRes leadingIcon: Int?,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        leadingIcon = leadingIcon?.let { id ->
            {
                Icon(
                    painter = painterResource(id),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        maxLines = 12,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePicker(
    onDatePicked: (LocalDate) -> Unit
) {
    val useCaseState = rememberUseCaseState(visible = true, onCloseRequest = { })
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    CalendarDialog(
        state = useCaseState,
        config = CalendarConfig(
            yearSelection = true,
            monthSelection = true,
            style = CalendarStyle.MONTH
        ),
        selection = CalendarSelection.Date { date ->
            selectedDate = date
            onDatePicked(date)
        }
    )
}


@Preview
@Composable
fun TextFieldCustomIconPreview() {
    TipTrackerTheme(darkTheme = true) {
        AppTextField(
            leadingIcon = R.drawable.receipt_long,
            label = "Super long label even longer than than this",
            value = "1",
            onValueChange = {},
            modifier = Modifier.width(160.dp)
        )
    }
}