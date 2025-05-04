package com.example.tiptracker.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tiptracker.ui.AppViewModelProvider
import com.example.tiptracker.ui.home.screens.BillScreen
import com.example.tiptracker.ui.home.screens.DescriptionScreen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel : LogFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
    modifier: Modifier = Modifier
) {
    val state = viewModel.formState

    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "Bill") {
        composable("Bill") {
            BillScreen(
                state = state,
                onValueChange = viewModel::updateUiState,
                onClearButtonClick = viewModel::clearState,
                onLogButtonClick = { navController.navigate("Review") },
                modifier = modifier
            )
        }
        composable("Review") {
            DescriptionScreen(
                state = state,
                onValueChange = viewModel::updateUiState,
                onBackButtonClick = { navController.navigate("Bill") },
                onSaveButtonClick = {
                    coroutineScope.launch {
                        viewModel.saveLog()
                    }
                },
                modifier = modifier
            )
        }
    }
}