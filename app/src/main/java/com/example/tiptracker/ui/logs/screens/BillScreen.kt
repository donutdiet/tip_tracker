package com.example.tiptracker.ui.logs.screens

import com.example.tiptracker.ui.logs.LogFormState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.R
import com.example.tiptracker.ui.components.AppButton
import com.example.tiptracker.ui.components.AppButtonOutlined
import com.example.tiptracker.ui.components.AppTextField
import com.example.tiptracker.ui.components.AppTextFieldDone
import com.example.tiptracker.ui_old.util.formatCurrency

@Composable
fun BillScreen(
    state: LogFormState,
    updateBill: (String) -> Unit,
    updateTipPercent: (String) -> Unit,
    updatePartySize: (String) -> Unit,
    updateRoundUpTip: (Boolean) -> Unit,
    updateRoundUpTotal: (Boolean) -> Unit,
    onClearButtonClick: () -> Unit,
    onLogButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                leadingIcon = R.drawable.receipt_long,
                label = "Bill Amount",
                value = state.bill,
                onValueChange = { updateBill(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                AppTextField(
                    leadingIcon = R.drawable.percent,
                    label = "Tip Percent",
                    value = state.tipPercent,
                    onValueChange = { updateTipPercent(it) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .weight(1.25f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppTextFieldDone(
                    leadingIcon = R.drawable.person,
                    label = "Party Size",
                    value = state.partySize,
                    onValueChange = { updatePartySize(it) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Round up tip?",
                    style = MaterialTheme.typography.labelSmall
                )
                Switch(
                    checked = state.roundUpTip,
                    onCheckedChange = { updateRoundUpTip(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Round up total?",
                    style = MaterialTheme.typography.labelSmall
                )
                Switch(
                    checked = state.roundUpTotal,
                    onCheckedChange = { updateRoundUpTotal(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Tip",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = formatCurrency(state.calculatedTip),
                    style = MaterialTheme.typography.displayMedium
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = formatCurrency(state.calculatedTotal),
                    style = MaterialTheme.typography.displayMedium
                )
            }
            if (state.parsedPartySize > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(text = "Per Person",)
                    Text(text = formatCurrency(state.calculatedPerPerson),)
                }
            }
            Row(modifier = Modifier.padding(top = 16.dp)) {
                AppButtonOutlined(
                    text = "Clear",
                    enabled = true,
                    onClick = onClearButtonClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppButton(
                    text = "Log",
                    enabled = true,
                    onClick = onLogButtonClick,
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
fun HomeScreenPreview() {
    TipTrackerTheme {
        BillScreen(
            state = LogFormState(),
            updateBill = {},
            updateTipPercent = {},
            updatePartySize = {},
            updateRoundUpTip = {},
            updateRoundUpTotal = {},
            onClearButtonClick = {},
            onLogButtonClick = {}
        )
    }
}