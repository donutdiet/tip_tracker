package com.example.tiptracker.ui.logs.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tiptracker.AppViewModelProvider
import com.example.tiptracker.ui.logs.LogFormViewModel
import com.example.tiptracker.ui.logs.UiEvent
import com.example.tiptracker.ui.logs.UiIntent
import kotlinx.serialization.Serializable

@Composable
fun AddLogNavHost(
    viewModel : LogFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
    modifier: Modifier = Modifier
) {

    val backStack = rememberNavBackStack(Calculator)
    val snackBarHostState = remember { SnackbarHostState() }

    val state by viewModel.state.collectAsState()
    val event by viewModel.event.collectAsState(initial = UiEvent.Idle)

    LaunchedEffect(event) {
        when(event) {
            is UiEvent.Navigate -> {
                backStack.add((event as UiEvent.Navigate).route)
            }
            is UiEvent.Back -> {
                backStack.removeLastOrNull()
            }
//            is UiEvent.SwitchTab -> {
//                navController.navigate("Bill") {
//                    popUpTo(navController.graph.findStartDestination().id) {
//                        inclusive = true
//                    }
//                    launchSingleTop = true
//                }
//                switchTab((event as UiEvent.SwitchTab).tab)
//            }
            is UiEvent.ShowSnackbar -> {
                snackBarHostState.showSnackbar((event as UiEvent.ShowSnackbar).message)
            }
            UiEvent.Idle -> {}
        }
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Calculator> {
                BillScreen(
                    state = state,
                    updateBill = { viewModel.onIntent(UiIntent.EnterBill(it)) },
                    updateTipPercent = { viewModel.onIntent(UiIntent.EnterTipPercent(it)) },
                    updatePartySize = { viewModel.onIntent(UiIntent.EnterPartySize(it)) },
                    updateRoundUpTip = { viewModel.onIntent(UiIntent.EnterRoundUpTip(it)) },
                    updateRoundUpTotal = { viewModel.onIntent(UiIntent.EnterRoundUpTotal(it)) },
                    onClearButtonClick = { viewModel.onIntent(UiIntent.Clear) },
                    onLogButtonClick = { viewModel.onIntent(UiIntent.Log) },
                    modifier = modifier
                )
            }
            entry<Review> {
                DescriptionScreen(
                    state = state,
                    updateRestaurantName = { viewModel.onIntent(UiIntent.EnterRestaurantName(it)) },
                    updateRestaurantDescription = {
                        viewModel.onIntent(
                            UiIntent.EnterRestaurantDescription(
                                it
                            )
                        )
                    },
                    updateDate = { viewModel.onIntent(UiIntent.EnterDate(it)) },
                    onBackButtonClick = { viewModel.onIntent(UiIntent.Back) },
                    onSaveButtonClick = { viewModel.onIntent(UiIntent.Save) },
                    modifier = modifier
                )
            }
        }
    )
}

@Serializable
data object Calculator : NavKey

@Serializable
data object Review : NavKey

@Serializable
data object Rate : NavKey