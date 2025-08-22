package com.example.tiptracker.ui.logs

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tiptracker.AppViewModelProvider
import com.example.tiptracker.ui.logs.screens.AddLogNavHost
import com.example.tiptracker.ui.logs.screens.actions.LogDetailsScreen
import com.example.tiptracker.ui.logs.screens.actions.LogEditScreen
import com.example.tiptracker.ui.logs.screens.actions.LogsListScreen
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LogsNavHost(
    onScreenChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val backStack = rememberNavBackStack(LogList)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    val logs by viewModel.logs.collectAsState()

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        sceneStrategy = listDetailStrategy,
        transitionSpec = {
            // Global forward animation — slide in from right
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Global backward animation — slide in from left
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Global predictive back gesture animation
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {
            entry<LogList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "Choose a log to see details")
                        }
                    }
                )
            ) {
                onScreenChange("List")
                LogsListScreen(
                    logs = logs,
                    onItemClick = { id ->
                        val last = backStack.lastOrNull()
                        if (last is LogDetails) {
                            // Expanded screens that show list and detail side to side
                            backStack[backStack.lastIndex] = LogDetails(id)
                        } else {
                            backStack.add(LogDetails(id))
                        }
                    },
                    onAddLogClick = { backStack.add(LogAdd) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            entry<LogAdd>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) {
                onScreenChange("Add")
                AddLogNavHost()
            }
            entry<LogDetails>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { args ->
                onScreenChange("Details")
                LogDetailsScreen(
                    logId = args.logId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onEditClick = { backStack.add(LogEdit(args.logId)) },
                    showBackButton = listDetailStrategy.directive.maxHorizontalPartitions == 1
                )
            }
            entry<LogEdit>(
                metadata = ListDetailSceneStrategy.extraPane()
            ) { args ->
                LogEditScreen(
                    logId = args.logId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSaveClick = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

@Serializable
data object LogList : NavKey

@Serializable
data object LogAdd : NavKey

@Serializable
data class LogDetails(val logId: Int) : NavKey

@Serializable
data class LogEdit(val logId: Int) : NavKey