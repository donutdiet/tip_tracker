package com.example.tiptracker.ui.tabs.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.ui.tabs.home.pages.ReviewPage
import com.example.tiptracker.ui.tabs.home.pages.TipCalculatorPage
import com.example.tiptracker.utils.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val snackbarScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.ShowError -> {
                snackbarScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
            }

            is HomeEvent.LogSaved -> {
                currentPage = 0
                snackbarScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Log saved successfully!")
                }
            }
        }
    }

    BackHandler(enabled = currentPage > 0) {
        currentPage = 0
    }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState > initialState) {
                // Going forward through the form.
                slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
            } else {
                // Going backward through the form.
                slideInVertically { -it } + fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
            }
        }
    ) { page ->
        when (page) {
            0 -> {
                TipCalculatorPage(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    onContinue = { currentPage = 1 }
                )
            }

            1 -> {
                ReviewPage(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    onBack = { currentPage = 0 }
                )
            }
        }
    }
}