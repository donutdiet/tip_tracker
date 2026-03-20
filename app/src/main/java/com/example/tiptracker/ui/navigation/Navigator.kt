package com.example.tiptracker.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(private val state: NavigationState) {

    // ----- Tab layer -----
    fun switchTab(route: NavKey) {
        check(route in state.tabBackStacks.keys) { "Unknown tab route: $route" }
        state.topLevelRoute = route
    }

    fun navigateInTab(route: NavKey) {
        state.tabBackStacks[state.topLevelRoute]?.add(route)
    }

    // ----- Root layer (no scaffold chrome) -----
    fun openFullscreen(route: NavKey) {
        // Prevent stale root-level snackbars from resurfacing after fullscreen navigation.
        state.snackBarHostState.currentSnackbarData?.dismiss()
        state.rootBackStack.add(route)
    }

    // Unified back for NavDisplay(onBack = ...)
    fun goBack() {
        if (state.isFullScreen) {
            // Prevent stale root-level snackbars from resurfacing after fullscreen navigation.
            state.snackBarHostState.currentSnackbarData?.dismiss()
            state.rootBackStack.removeLastOrNull()
        } else {
            goBackInCurrentTab()
        }
    }

    private fun goBackInCurrentTab() {
        val currentStack = state.tabBackStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")

        val currentRoute = currentStack.last()
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startTabRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}