package com.example.tiptracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import com.example.tiptracker.ui.navigation.Navigator
import com.example.tiptracker.ui.navigation.NavigationState
import com.example.tiptracker.ui.navigation.rememberNavigationState
import com.example.tiptracker.ui.navigation.TOP_LEVEL_ROUTES
import androidx.navigation3.ui.NavDisplay
import com.example.tiptracker.R
import com.example.tiptracker.ui.navigation.RootKey
import com.example.tiptracker.ui.navigation.TabKey
import com.example.tiptracker.ui.navigation.rootEntries
import com.example.tiptracker.ui.navigation.tabEntries
import kotlinx.coroutines.launch

@Composable
fun RootActivity() {
    val navigationState = rememberNavigationState(
        appRoot = RootKey.Root,
        startRoute = TabKey.Home,
        topLevelRoutes = TOP_LEVEL_ROUTES.keys
    )
    val navigator = remember { Navigator(navigationState) }
    val rootScope = rememberCoroutineScope()

    val tabEntryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        tabEntries(
            openLogDetail = { navigator.openFullscreen(RootKey.LogDetail(it)) },
            openLogSaved = { navigator.onLogSaved(it) },
            snackbarHostState =  navigationState.snackBarHostState
        )
    }

    val rootEntryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<RootKey.Root> {
            AppRoot(
                navigationState = navigationState,
                navigator = navigator,
                tabEntryProvider = tabEntryProvider
            )
        }
        rootEntries(
            navigateBack = { navigator.goBack() },
            snackbarHostState = navigationState.snackBarHostState,
            onLogDeleted = {
                navigator.goBack()
                rootScope.launch {
                    navigationState.snackBarHostState.showSnackbar("Log deleted successfully!")
                }
            },
            onLogUpdated = {
                navigator.goBack()
                rootScope.launch {
                    navigationState.snackBarHostState.showSnackbar("Log updated successfully!")
                }
            },
            openEditPage = { navigator.openFullscreen(RootKey.EditLog(it)) },
            openImageManager = { navigator.openFullscreen(RootKey.LogImageManager(it)) },
            pop = { navigator.goBack() }
        )
    }

    val rootEntries = rememberDecoratedNavEntries(
        backStack = navigationState.rootBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = rootEntryProvider
    )

    NavDisplay(
        entries = rootEntries,
        onBack = { navigator.goBack() },
        transitionSpec = {
            fullScreenTransform(
                isPop = false,
                fromKey = initialState.topRootContentKey(),
                toKey = targetState.topRootContentKey()
            )
        },
        popTransitionSpec = {
            fullScreenTransform(
                isPop = true,
                fromKey = initialState.topRootContentKey(),
                toKey = targetState.topRootContentKey()
            )
        },
        predictivePopTransitionSpec = { _ ->
            fullScreenTransform(
                isPop = true,
                fromKey = initialState.topRootContentKey(),
                toKey = targetState.topRootContentKey()
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    navigationState: NavigationState,
    navigator: Navigator,
    tabEntryProvider: (NavKey) -> NavEntry<NavKey>,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tip Tracker", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { navigator.openFullscreen(RootKey.Settings) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                TOP_LEVEL_ROUTES.forEach { (key, value) ->
                    val isSelected = key == navigationState.topLevelRoute
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigator.switchTab(key) },
                        icon = {
                            Icon(
                                painter = painterResource(id = value.iconRes),
                                contentDescription = value.description,
                            )
                        },
                        label = { Text(text = value.description) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(navigationState.snackBarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavDisplay(
                entries = navigationState.toDecoratedTabEntries(tabEntryProvider),
                onBack = { navigator.goBack() },
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ -> EnterTransition.None togetherWith ExitTransition.None }
            )
        }
    }
}

private fun Scene<NavKey>.topRootContentKey(): String? =
    entries.asReversed().firstOrNull()?.contentKey?.toString()

private fun fullScreenTransform(
    isPop: Boolean,
    fromKey: String?,
    toKey: String?
) =
    // HACK: Super scuffed way to configure different transitions for screens
    if (fromKey?.startsWith("LogSaved") == true || toKey?.startsWith("LogSaved") == true) {
        slideInVertically(initialOffsetY = { it }) togetherWith
            slideOutVertically(targetOffsetY = { -it })
    } else if (isPop) {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
    } else {
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
    }