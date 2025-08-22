package com.example.tiptracker.ui_old.addentry

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import androidx.lifecycle.viewmodel.compose.viewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiningDescriptionScreen(
    modifier: Modifier = Modifier,
    onCancelButtonClicked: () -> Unit = {},
    onSaveButtonClicked: () -> Unit = {},
    viewModel: LogViewModel,
) {
//    val calendarState = rememberSheetState()
//
//    CalendarDialog(
//        state = calendarState,
//        config = CalendarConfig(
//            monthSelection = true,
//            yearSelection = true
//        ),
//        selection = CalendarSelection.Date { date ->
//            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
//            viewModel.onDateChange(date.format(formatter))
//        }
//    )

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Describe your experience",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextInputField(
                label = R.string.restaurant_name,
                leadingIcon = R.drawable.storefront,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                value = viewModel.restaurantName.value,
                onValueChange = { viewModel.onRestaurantNameChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            MultiLineTextInputField(
                label = R.string.restaurant_description,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                value = viewModel.restaurantDescription.value,
                onValueChange = { viewModel.onRestaurantDescriptionChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
            )
//            DatePickerDisplay(
//                viewModel = viewModel,
//                calendarState = calendarState,
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
            FinalBill(
                tip = viewModel.getCalculatedTip(),
                total = viewModel.getCalculatedTotal(),
                personCount = viewModel.personCount.value.toIntOrNull() ?: 1,
                totalPerPerson = viewModel.getCalculatedTotalPerPerson(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            TwoButtonRow(
                buttonLabelL = R.string.cancel,
                buttonLabelR = R.string.save,
                onButtonClickL = onCancelButtonClicked,
                onButtonClickR = onSaveButtonClicked
            )
        }
    }
}

@Composable
fun MultiLineTextInputField(
    @StringRes label: Int,
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
                stringResource(label),
                style = MaterialTheme.typography.labelSmall
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        modifier = modifier
            .heightIn(min = 200.dp)
            .focusRequester(focusRequester),
        maxLines = 12
    )
}

//@Composable
//fun DatePickerDisplay(
//    viewModel: LogViewModel,
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
//            text = viewModel.date.value,
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
fun DiningDescriptionScreenPreview() {
    TipTrackerTheme {
        DiningDescriptionScreen(viewModel = viewModel())
    }
}

//@RequiresApi(Build.VERSION_CODES.O)
//@Preview(showBackground = true)
//@Composable
//fun DiningDescriptionScreenPreviewDarkMode() {
//    TipTrackerTheme(darkTheme = true) {
//        DiningDescriptionScreen(viewModel = viewModel())
//    }
//}