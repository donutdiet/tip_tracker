package com.example.tiptracker.ui.tabs.home

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable sealed interface HomeKey : NavKey {
    @Serializable data object TipCalculatorPage : HomeKey
    @Serializable data object ReviewPage : HomeKey
}

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = koinViewModel()
) {

}