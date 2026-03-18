package com.example.tiptracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

@Composable
fun RootActivity() {
    val navigationState = rememberNavigationState(
        appRoot = RootKey.Root,
        startRoute = TabKey.Home,
        topLevelRoutes = TOP_LEVEL_ROUTES.keys
    )
    val navigator = remember { Navigator(navigationState) }

    val tabEntryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        tabEntries()
    }

    val rootEntryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<RootKey.Root> {
            AppRoot(
                navigationState = navigationState,
                navigator = navigator,
                tabEntryProvider = tabEntryProvider
            )
        }
        rootEntries(navigateBack = { navigator.goBack() })
    }

    val rootEntries = rememberDecoratedNavEntries(
        backStack = navigationState.rootBackStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = rootEntryProvider
    )

    NavDisplay(
        entries = rootEntries,
        onBack = { navigator.goBack() },
        transitionSpec = { fullScreenTransform(isPop = false) },
        popTransitionSpec = { fullScreenTransform(isPop = true) },
        predictivePopTransitionSpec = { _ -> fullScreenTransform(isPop = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    navigationState: NavigationState,
    navigator: Navigator,
    tabEntryProvider: (NavKey) -> NavEntry<NavKey>,
) {
    val topLevelPositionByContentKey = remember {
        TOP_LEVEL_ROUTES
            .mapKeys { (tabKey, _) -> tabKey.toString() }
            .mapValues { (_, item) -> item.position }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tip Tracker") },
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
            NavigationBar {
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
                        label = { Text(text = value.description) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavDisplay(
                entries = navigationState.toDecoratedTabEntries(tabEntryProvider),
                onBack = { navigator.goBack() },
                transitionSpec = {
                    horizontalTabTransform(
                        fromPosition = initialState.topLevelPosition(topLevelPositionByContentKey),
                        toPosition = targetState.topLevelPosition(topLevelPositionByContentKey)
                    )
                },
                popTransitionSpec = {
                    horizontalTabTransform(
                        fromPosition = initialState.topLevelPosition(topLevelPositionByContentKey),
                        toPosition = targetState.topLevelPosition(topLevelPositionByContentKey)
                    )
                },
                predictivePopTransitionSpec = { _ ->
                    horizontalTabTransform(
                        fromPosition = initialState.topLevelPosition(topLevelPositionByContentKey),
                        toPosition = targetState.topLevelPosition(topLevelPositionByContentKey)
                    )
                }
            )
        }
    }
}

private fun Scene<NavKey>.topLevelPosition(positionByContentKey: Map<String, Int>): Int? =
    entries.asReversed().firstNotNullOfOrNull { entry ->
        positionByContentKey[entry.contentKey.toString()]
    }

private fun horizontalTabTransform(fromPosition: Int?, toPosition: Int?) =
    when {
        fromPosition == null || toPosition == null || fromPosition == toPosition ->
            EnterTransition.None togetherWith ExitTransition.None
        toPosition > fromPosition ->
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        else ->
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
    }

private fun fullScreenTransform(isPop: Boolean) =
    if (isPop) {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
    } else {
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
    }