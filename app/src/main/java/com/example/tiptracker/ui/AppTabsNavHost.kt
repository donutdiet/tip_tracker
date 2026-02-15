package com.example.tiptracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tiptracker.ui.tabs.AppBottomNavigation
import com.example.tiptracker.ui.logs.LogsNavHost
import com.example.tiptracker.ui.profile.ProfileScreen
import com.example.tiptracker.ui.screens.rankings.RankingsScreen
import com.example.tiptracker.ui.tabs.TabBarItem
import com.example.tiptracker.ui.tabs.getIndexForTab

@Composable
fun AppTabsNavHost() {
    val navController = rememberNavController()

    // Not using Serializable navigation b/c index prop is needed for transitions
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var currentTabIndex by remember { mutableIntStateOf(TabBarItem.Logs.index) }
    var previousTabIndex by remember { mutableIntStateOf(TabBarItem.Logs.index) }

    var showAppBars by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        val newCurrentIndex = getIndexForTab(currentRoute)
        if (newCurrentIndex != currentTabIndex) {
            previousTabIndex = currentTabIndex
            currentTabIndex = newCurrentIndex
        }
    }

    Scaffold(
        bottomBar = {
            if (showAppBars) {
                AppBottomNavigation(
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        NavHost(
            navController = navController,
            startDestination = TabBarItem.Logs.title,
            enterTransition = {
                if (currentTabIndex > previousTabIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (currentTabIndex > previousTabIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                if (currentTabIndex > previousTabIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            popExitTransition = {
                if (currentTabIndex > previousTabIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            }
        ) {
            composable(route = TabBarItem.Logs.title) {
                LogsNavHost(
                    onScreenChange = { screen ->
                        showAppBars = screen == "List"
                    },
                    modifier = screenModifier
                )
            }
            composable(route = TabBarItem.Rankings.title) {
                RankingsScreen(
                    modifier = screenModifier
                )
            }
            composable(route = TabBarItem.Profile.title) {
                ProfileScreen(
                    modifier = screenModifier
                )
            }
        }
    }
}