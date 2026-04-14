package com.example.tiptracker.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.example.tiptracker.R
import kotlinx.serialization.Serializable

@Serializable sealed interface RootKey : NavKey {
    @Serializable data object Root : RootKey  // Contains bottom nav bar tabs

    // Full screen pages
    @Serializable data object Settings : RootKey

    @Serializable data class LogSaved(val id: Int) : RootKey

    @Serializable data class LogDetail(val id: Int) : RootKey

    @Serializable data class EditLog(val id: Int) : RootKey
}

@Serializable sealed interface TabKey : NavKey {
    @Serializable data object Logs : TabKey
    @Serializable data object Home : TabKey
    @Serializable data object Profile : TabKey
}

data class NavBarItem(
    val iconRes: Int,
    val description: String,
    val position: Int
)

val TOP_LEVEL_ROUTES = mapOf<NavKey, NavBarItem>(
    TabKey.Logs to NavBarItem(iconRes = R.drawable.list, description = "Logs", 0),
    TabKey.Home to NavBarItem(iconRes = R.drawable.add, description = "New", 1),
    TabKey.Profile to NavBarItem(iconRes = R.drawable.person, description = "Profile", 2)
)