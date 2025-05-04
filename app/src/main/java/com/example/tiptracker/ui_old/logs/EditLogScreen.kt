package com.example.tiptracker.ui_old.logs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui_old.addentry.PageHeader
import com.example.tiptracker.ui_old.theme.TipTrackerTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiptracker.ui_old.addentry.MultiLineTextInputField
import com.example.tiptracker.ui_old.addentry.TextInputField
import com.example.tiptracker.ui_old.addentry.TwoButtonRow
import java.text.NumberFormat

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogScreen(
    modifier: Modifier = Modifier,
    onCancelButtonClicked: () -> Unit,
    onSaveButtonClicked: () -> Unit,
    editLogViewModel: EditLogViewModel
) {
//    val calendarState = rememberSheetState()

//    CalendarDialog(
//        state = calendarState,
//        config = CalendarConfig(
//            monthSelection = true,
//            yearSelection = true
//        ),
//        selection = CalendarSelection.Date { date ->
//            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
//            editLogViewModel.onDateChange(date.format(formatter))
//        }
//    )

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PageHeader(
                textId = R.string.edit_dining_entry,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextInputField(
                label = R.string.restaurant_name,
                leadingIcon = R.drawable.storefront,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                value = editLogViewModel.tempRestaurantName.value,
                onValueChange = { editLogViewModel.onRestaurantNameChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            MultiLineTextInputField(
                label = R.string.restaurant_description,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                value = editLogViewModel.tempRestaurantDescription.value,
                onValueChange = { editLogViewModel.onRestaurantDescriptionChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
            BillEditForm(
                editLogViewModel = editLogViewModel,
                billAmount = editLogViewModel.tempBillAmount.value,
                tipAmount = editLogViewModel.tempTipAmount.value,
                personCount = editLogViewModel.tempPersonCount.value
            )
//            DatePickerDisplay(
//                viewModel = editLogViewModel,
//                calendarState = calendarState,
//                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
//            )
            FinalBill(
                tip = editLogViewModel.tempTipAmount.value.toDoubleOrNull() ?: 0.0,
                total = editLogViewModel.getCalculatedTotal(),
                personCount = editLogViewModel.tempPersonCount.value.toIntOrNull() ?: 1,
                totalPerPerson = editLogViewModel.getCalculatedTotalPerPerson()
            )
            TwoButtonRow(
                buttonLabelL = R.string.cancel,
                buttonLabelR = R.string.save,
                onButtonClickL = onCancelButtonClicked,
                onButtonClickR = onSaveButtonClicked,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun BillEditForm(
    editLogViewModel: EditLogViewModel,
    billAmount: String,
    tipAmount: String,
    personCount: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TextInputField(
            label = R.string.bill_amount,
            leadingIcon = R.drawable.receipt_long,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            value = billAmount,
            onValueChange = { editLogViewModel.onBillAmountChange(it) },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
        ) {
            TextInputField(
                label = R.string.tip_amount,
                leadingIcon = R.drawable.money,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                value = tipAmount,
                onValueChange = { editLogViewModel.onTipAmountChange(it) },
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
                onValueChange = { editLogViewModel.onPersonCountChange(it) },
                modifier = Modifier.weight(1f)
            )
        }
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

//@Composable
//fun DatePickerDisplay(
//    viewModel: EditLogViewModel,
//    calendarState: SheetState,
//    modifier: Modifier = Modifier
//) {
//    val coroutineScope = rememberCoroutineScope()
//
//    Row(
//        horizontalArrangement = Arrangement.Center,
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = modifier.fillMaxHeight()
//    ) {
//        Text(
//            text = "Date: ",
//            style = MaterialTheme.typography.bodyLarge
//        )
//        Text(
//            text = viewModel.tempDate.value,
//            style = MaterialTheme.typography.labelMedium
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        OutlinedButton(
//            onClick = {
//                coroutineScope.launch {
//                    calendarState.show()
//                }
//            },
//            shape = RoundedCornerShape(4.dp),
//            modifier = Modifier.height(40.dp)
//        ) {
//            Text(
//                text = "Edit date",
//                style = MaterialTheme.typography.labelSmall
//            )
//        }
//    }
//}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun EditLogScreenPreview() {
    TipTrackerTheme {
        EditLogScreen(
            editLogViewModel = viewModel(),
            onCancelButtonClicked = {},
            onSaveButtonClicked = {},
        )
    }
}