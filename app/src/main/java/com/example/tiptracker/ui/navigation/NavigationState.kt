package com.example.tiptracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

@Composable
fun rememberNavigationState(
    appRoot: NavKey,
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    // Create a back stack for each top level route.
    val tabBackStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    val rootBackStack = rememberNavBackStack(appRoot)

    return remember(appRoot, startRoute, topLevelRoutes) {
        NavigationState(
            appRoot = appRoot,
            startTabRoute = startRoute,
            topLevelRoute = topLevelRoute,
            tabBackStacks = tabBackStacks,
            rootBackStack = rootBackStack
        )
    }
}

/**
 * State holder for navigation state. This class does not modify its own state. It is designed
 * to be modified using the `Navigator` class.
 *
 * @param appRoot - the key for the root route.
 * @param startTabRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the state object that backs the top level route.
 * @param tabBackStacks - the back stacks for each top level route.
 * @param rootBackStack - the back stack for the root route.
 */
class NavigationState(
    val appRoot: NavKey,
    val startTabRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val tabBackStacks: Map<NavKey, NavBackStack<NavKey>>,
    val rootBackStack: NavBackStack<NavKey>
) {

    /**
     * The top level route.
     */
    var topLevelRoute: NavKey by topLevelRoute

    val isFullScreen: Boolean
        get() = rootBackStack.last() != appRoot

    /**
     * Convert the navigation state into `NavEntry`s that have been decorated with a
     * `SaveableStateHolder`.
     *
     * @param entryProvider - the entry provider used to convert the keys in the
     * back stacks to `NavEntry`s.
     */
    @Composable
    fun toDecoratedTabEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>
    ): List<NavEntry<NavKey>> {

        // For each back stack, create a `SaveableStateHolder` decorator and use it to decorate
        // the entries from that stack. When backStacks changes, `rememberDecoratedNavEntries` will
        // be recomposed and a new list of decorated entries is returned.
        val decoratedEntries = tabBackStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }

        // Only return the entries for the stacks that are currently in use.
        return getTopLevelRoutesInUse()
            .flatMap { decoratedEntries[it] ?: emptyList() }
    }

    /**
     * Get the top level routes that are currently in use. The start route is always the first route
     * in the list. This means the user will always exit the app through the starting route
     * ("exit through home" pattern). The list will contain a maximum of one other route. This is a
     * design decision. In your app, you may wish to allow more than two top level routes to be
     * active.
     *
     * Note that even if a top level route is not in use its state is still retained.
     *
     * @return the current top level routes that are in use.
     */
    private fun getTopLevelRoutesInUse() : List<NavKey> =
        if (topLevelRoute == startTabRoute) {
            listOf(startTabRoute)
        } else {
            listOf(startTabRoute, topLevelRoute)
        }
}