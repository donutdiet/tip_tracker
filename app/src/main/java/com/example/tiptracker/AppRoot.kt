package com.example.tiptracker

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiptracker.ui_old.navigation.AppBottomNavigation
import androidx.navigation.compose.rememberNavController
import com.example.tiptracker.ui.home.HomeScreen
import com.example.tiptracker.ui.home.LogFormViewModel
import com.example.tiptracker.ui.logs.LogsScreen
import com.example.tiptracker.ui.profile.ProfileScreen
import com.example.tiptracker.ui.rankings.RankingsScreen
import com.example.tiptracker.ui.settings.SettingsScreen


@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 2)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current


    Scaffold(
        bottomBar = {
            AppBottomNavigation(pagerState, scope)
        }
    ) { innerPadding ->
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> RankingsScreen(
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> LogsScreen(
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> HomeScreen(
                    modifier = Modifier.padding(innerPadding)
                )
                3 -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding)
                )
                4 -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

    }
}