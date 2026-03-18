package com.example.tiptracker.ui.tabs.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
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
import com.example.tiptracker.utils.ObserveAsEvents
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable sealed interface HomeKey : NavKey {
    @Serializable data object TipCalculatorPage : HomeKey
    @Serializable data object ReviewPage : HomeKey
}

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(HomeKey.TipCalculatorPage)

    val popLocalBackStack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
    val resetToStartPage: () -> Unit = {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message)
            }
            is HomeEvent.LogSaved -> {
                resetToStartPage()
                snackbarHostState.showSnackbar("Log saved successfully!")
            }
        }
    }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<HomeKey.TipCalculatorPage> {
            TipCalculatorPage(
                uiState = uiState,
                onAction = viewModel::onAction,
                onContinue = {
                    if (backStack.lastOrNull() != HomeKey.ReviewPage) {
                        backStack.add(HomeKey.ReviewPage)
                    }
                }
            )
        }

        entry<HomeKey.ReviewPage> {
            ReviewPage(
                uiState = uiState,
                onAction = viewModel::onAction,
                onBack = popLocalBackStack
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
        onBack = popLocalBackStack,
        transitionSpec = {
            slideInVertically(initialOffsetY = { it }) togetherWith
                    slideOutVertically(targetOffsetY = { -it })
        },
        popTransitionSpec = {
            slideInVertically(initialOffsetY = { -it }) togetherWith
                    slideOutVertically(targetOffsetY = { it})
        },
        predictivePopTransitionSpec = {
            slideInVertically(initialOffsetY = { -it }, animationSpec = tween(3000)) togetherWith
                    slideOutVertically(targetOffsetY = { it}, animationSpec = tween(3000))
        }
    )
}