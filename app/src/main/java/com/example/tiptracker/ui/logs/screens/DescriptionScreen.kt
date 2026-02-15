package com.example.tiptracker.ui.logs.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.logs.LogFormState
import com.example.tiptracker.ui.components.AppButton
import com.example.tiptracker.ui.components.AppButtonOutlined
import com.example.tiptracker.ui.components.AppMultiLineTextFieldDone
import com.example.tiptracker.ui.components.AppTextField
import com.example.tiptracker.ui.components.MyDatePicker
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui_old.util.formatCurrency
import com.example.tiptracker.ui_old.util.formatDate
import java.time.LocalDate

@Composable
fun DescriptionScreen(
    state: LogFormState,
    updateRestaurantName: (String) -> Unit,
    updateRestaurantDescription: (String) -> Unit,
    updateDate: (LocalDate) -> Unit,
    onBackButtonClick: () -> Unit,
    onSaveButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCalendarDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
        ) {
            AppTextField(
                leadingIcon = R.drawable.storefront,
                label = "Restaurant Name",
                value = state.restaurantName,
                onValueChange = { updateRestaurantName(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            )
            AppMultiLineTextFieldDone(
                leadingIcon = null,
                label = "Leave a review",
                value = state.restaurantDescription,
                onValueChange = { updateRestaurantDescription(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
            )
            OutlinedTextField(
                value = formatDate(state.date),
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pick a date",
                        modifier = Modifier.clickable(onClick = { showCalendarDialog = true })
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
            if (showCalendarDialog) {
                MyDatePicker(
                    onDatePicked = { date ->
                        updateDate(date)
                        showCalendarDialog = false
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Bill")
                Text(text = formatCurrency(state.parsedBill))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (state.roundUpTip || state.roundUpTotal) {
                    Text(text = "Tip % (rounded)")
                } else {
                    Text(text = "Tip %")
                }
                Text(text = state.trueTipPercent.toString() + "%")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Party Size")
                Text(text = state.parsedPartySize.toString())
            }
            HorizontalDivider(
                thickness = 2.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Tip")
                Text(text = formatCurrency(state.calculatedTip))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Total")
                Text(text = formatCurrency(state.calculatedTotal))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "Per Person")
                Text(text = formatCurrency(state.calculatedPerPerson))
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                AppButtonOutlined(
                    text = "Back",
                    enabled = true,
                    onClick = onBackButtonClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppButton(
                    text = "Save",
                    enabled = true,
                    onClick = onSaveButtonClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewScreenPreview() {
    TipTrackerTheme {
        DescriptionScreen(
            state = LogFormState(),
            updateRestaurantName = {},
            updateRestaurantDescription = {},
            updateDate = {},
            onBackButtonClick = {},
            onSaveButtonClick = {},
        )
    }
}