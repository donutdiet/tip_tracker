package com.example.tiptracker.ui.tabs.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.tiptracker.ui.tabs.home.pages.ReviewPage
import com.example.tiptracker.ui.tabs.home.pages.TipCalculatorPage
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(HomeKey.TipCalculatorPage)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<HomeKey.TipCalculatorPage> {
            TipCalculatorPage(
                uiState = uiState,
                onAction = viewModel::onAction,
                onContinue = { backStack.add(HomeKey.ReviewPage) }
            )
        }

        entry<HomeKey.ReviewPage> {
            ReviewPage(
                uiState = uiState,
                onAction = viewModel::onAction,
                onBack = { backStack.removeLastOrNull() }
            )
        }
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider
    )

    NavDisplay(
        entries = entries,
        onBack = { backStack.removeLastOrNull() }
    )
}