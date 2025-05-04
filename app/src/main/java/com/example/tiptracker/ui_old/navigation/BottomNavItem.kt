package com.example.tiptracker.ui_old.navigation

import androidx.annotation.DrawableRes
import com.example.tiptracker.R

sealed class BottomNavItem(val route: String, val label: String, @DrawableRes val icon: Int) {
    object Rankings : BottomNavItem("rankings", "Rankings", R.drawable.leaderboard)
    object Logs : BottomNavItem("logs", "Logs", R.drawable.list)
    object Home : BottomNavItem("home", "New", R.drawable.add)
    object Profile : BottomNavItem("profile", "Profile", R.drawable.person)
    object Settings : BottomNavItem("settings", "Settings", R.drawable.settings)
}

val items = listOf(
    BottomNavItem.Rankings,
    BottomNavItem.Logs,
    BottomNavItem.Home,
    BottomNavItem.Profile,
    BottomNavItem.Settings
)