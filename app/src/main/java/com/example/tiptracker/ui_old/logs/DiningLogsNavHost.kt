package com.example.tiptracker.ui_old.logs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tiptracker.ui_old.addentry.LogViewModel

enum class DiningLogsScreens {
    DiningLogs,
    EditLogs
}

@Composable
fun DiningLogsNavHost(
    logViewModel: LogViewModel,
    editLogViewModel: EditLogViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DiningLogsScreens.DiningLogs.name,
        modifier = modifier
    ) {
        composable(route = DiningLogsScreens.DiningLogs.name) {
            DiningLogsScreen(
                diningLogs = logViewModel.diningLogs,
                logViewModel = logViewModel,
                onFavoriteButtonClicked = {
                    logViewModel.onFavoriteButtonClicked(it)
                },
                onEditButtonClicked = {
                    editLogViewModel.loadCurrentLogData(
                        diningLogs = logViewModel.diningLogs,
                        index = it
                    )
                    navController.navigate(DiningLogsScreens.EditLogs.name)
                },
                onDeleteButtonClicked = {
                    logViewModel.deleteEntry(it)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(route = DiningLogsScreens.EditLogs.name) {
            EditLogScreen(
                onCancelButtonClicked = {
                    editLogViewModel.clearForm()
                    navController.navigate(DiningLogsScreens.DiningLogs.name)
                },
                onSaveButtonClicked = {
                    logViewModel.updateEntry(
                        id = editLogViewModel.id,
                        newBillAmount = editLogViewModel.tempBillAmount.value,
                        newTipPercent = editLogViewModel.getCalculatedTipPercent(),
                        newTipAmount = editLogViewModel.tempTipAmount.value,
                        newPersonCount = editLogViewModel.tempPersonCount.value,
                        newTotalAmount = editLogViewModel.getCalculatedTotal(),
                        newTotalAmountPerPerson = editLogViewModel.getCalculatedTotalPerPerson(),
                        newRestaurantName = editLogViewModel.tempRestaurantName.value,
                        newRestaurantDescription = editLogViewModel.tempRestaurantDescription.value,
                        newDate = editLogViewModel.tempDate.value
                    )
                    editLogViewModel.clearForm()
                    navController.navigate(DiningLogsScreens.DiningLogs.name)
                },
                editLogViewModel = editLogViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}