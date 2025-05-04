package com.example.tiptracker.ui_old.addentry

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class DiningInputScreens {
    BillInput,
    DescriptionInput
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEntryFormNavHost(
    viewModel: LogViewModel,
    navigateToDiningLogsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DiningInputScreens.BillInput.name,
        modifier = modifier
    ) {
        composable(
            route = DiningInputScreens.BillInput.name,
        ) {
            BillInputScreen(
                modifier = Modifier.fillMaxSize(),
                onClearButtonClicked = { viewModel.clearForm() },
                onLogButtonClicked = {
                    if(viewModel.checkFormValidity()) {
                        viewModel.updateCurrentDate()
                        navController.navigate(DiningInputScreens.DescriptionInput.name)
                    }
                },
                viewModel = viewModel
            )
        }
        composable(route = DiningInputScreens.DescriptionInput.name) {
            DiningDescriptionScreen(
                modifier = Modifier.fillMaxSize(),
                onCancelButtonClicked = {
                    navController.popBackStack(
                        DiningInputScreens.BillInput.name,
                        inclusive = false
                    )
                },
                onSaveButtonClicked = {
                    viewModel.addLogEntry()
                    navController.popBackStack(
                        DiningInputScreens.BillInput.name,
                        inclusive = false
                    )
                    navigateToDiningLogsScreen()
                },
                viewModel = viewModel
            )
        }
    }
}