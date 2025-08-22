package com.example.tiptracker.ui_old

import androidx.annotation.DrawableRes
import com.example.tiptracker.R

enum class AppScreens {
    RankingsScreen,
    LogsScreen,
    HomeScreen,
    ProfileScreen,
    SettingsScreen,
}

data class BottomBarNavItem(
    val label: String,
    @DrawableRes val iconRes: Int,
    val route: String,
)

val navItemList = listOf(
    BottomBarNavItem(
        label = "Rankings",
        iconRes = R.drawable.ic_leaderboard_outline,
        route = AppScreens.RankingsScreen.name
    ),
    BottomBarNavItem(
        label = "Logs",
        iconRes = R.drawable.ic_leaderboard_filled,
        route = AppScreens.LogsScreen.name
    ),
    BottomBarNavItem(
        label = "New",
        iconRes = R.drawable.ic_add_outline,
        route = AppScreens.HomeScreen.name
    ),
    BottomBarNavItem(
        label = "Profile",
        iconRes = R.drawable.person,
        route = AppScreens.ProfileScreen.name
    ),
    BottomBarNavItem(
        label = "Settings",
        iconRes = R.drawable.settings,
        route = AppScreens.SettingsScreen.name
    ),
)