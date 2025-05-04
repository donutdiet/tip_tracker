package com.example.tiptracker.ui_old.addentry


import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.ui_old.theme.TipTrackerTheme
import java.text.NumberFormat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiptracker.R

@Composable
fun BillInputScreen(
    modifier: Modifier = Modifier,
    onClearButtonClicked: () -> Unit = {},
    onLogButtonClicked: () -> Unit = {},
    viewModel: LogViewModel,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PageHeader(
                textId = R.string.calculate_tips,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            BillInputForm(
                viewModel = viewModel,
                billAmount = viewModel.billAmount.value,
                tipPercent = viewModel.tipPercent.value,
                personCount = viewModel.personCount.value
            )
            RoundUpRow(
                label = R.string.round_up_tip,
                roundUp = viewModel.roundUpTip.value,
                onRoundUpChanged = { viewModel.onRoundUpTipChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            RoundUpRow(
                label = R.string.round_up_total,
                roundUp = viewModel.roundUpTotal.value,
                onRoundUpChanged = { viewModel.onRoundUpTotalChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FinalBill(
                tip = viewModel.getCalculatedTip(),
                total = viewModel.getCalculatedTotal(),
                personCount = viewModel.personCount.value.toIntOrNull() ?: 1,
                totalPerPerson = viewModel.getCalculatedTotalPerPerson(),
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
            )
            TwoButtonRow(
                buttonLabelL = R.string.clear,
                buttonLabelR = R.string.log,
                onButtonClickL = onClearButtonClicked,
                onButtonClickR = onLogButtonClicked
            )
        }
    }
}

@Composable
fun PageHeader(
    @StringRes textId: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(textId),
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
fun BillInputForm(
    viewModel: LogViewModel,
    billAmount: String,
    tipPercent: String,
    personCount: String
) {
    TextInputField(
        label = R.string.bill_amount,
        leadingIcon = R.drawable.receipt_long,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        value = billAmount,
        onValueChange = { viewModel.onBillAmountChange(it) },
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
    )
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
    ) {
        TextInputField(
            label = R.string.tip_percentage,
            leadingIcon = R.drawable.percent,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            value = tipPercent,
            onValueChange = { viewModel.onTipPercentChange(it) },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        TextInputField(
            label = R.string.people,
            leadingIcon = R.drawable.person,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            value = personCount,
            onValueChange = { viewModel.onPersonCountChange(it) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RoundUpRow(
    @StringRes label: Int,
    roundUp: Boolean,
    onRoundUpChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall
        )
        Switch(
            checked = roundUp,
            onCheckedChange = onRoundUpChanged,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.End)
        )
    }
}

@Composable
fun FinalBill(
    tip: Double,
    total: Double,
    personCount: Int,
    totalPerPerson: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.tip),
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = stringResource(
                    R.string.money_string,
                    NumberFormat.getCurrencyInstance().format(tip)
                ),
                style = MaterialTheme.typography.displayMedium
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.total),
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = stringResource(
                    R.string.money_string,
                    NumberFormat.getCurrencyInstance().format(total)
                ),
                style = MaterialTheme.typography.displayMedium
            )
        }
        if (personCount > 1) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.per_person),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = stringResource(
                        R.string.money_string,
                        NumberFormat.getCurrencyInstance().format(totalPerPerson)
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun TwoButtonRow(
    @StringRes buttonLabelL: Int,
    @StringRes buttonLabelR: Int,
    onButtonClickL: () -> Unit,
    onButtonClickR: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = onButtonClickL,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Text(
                text = stringResource(buttonLabelL),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onButtonClickR,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .height(48.dp)
                .weight(1f)
        ) {
            Text(text = stringResource(buttonLabelR))
        }
    }
}

@Composable
fun TextInputField(
    @StringRes label: Int,
    @DrawableRes leadingIcon: Int,
    keyboardOptions: KeyboardOptions,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        label = {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = MaterialTheme.typography.labelMedium,
        leadingIcon = {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        singleLine = true,
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TipTrackerPreview() {
    TipTrackerTheme {
        BillInputScreen(viewModel = viewModel())
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun TipTrackerPreviewDarkTheme() {
//    TipTrackerTheme(darkTheme = true) {
//        BillInputScreen(viewModel = viewModel())
//    }
//}