package com.example.tiptracker.ui.tabs

import androidx.annotation.DrawableRes
import com.example.tiptracker.R

sealed class TabBarItem(
    val title: String,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int,
    val index: Int,
    val badgeCount: Int? = null
) {
    object Logs: TabBarItem(
        title = "Logs",
        selectedIcon = R.drawable.ic_list_filled,
        unselectedIcon = R.drawable.ic_list_outline,
        index = 0
    )
    object Rankings: TabBarItem(
        title = "Rankings",
        selectedIcon = R.drawable.ic_leaderboard_filled,
        unselectedIcon = R.drawable.ic_leaderboard_outline,
        index = 1,
        badgeCount = 7
    )
    object Profile: TabBarItem(
        title = "Profile",
        selectedIcon = R.drawable.ic_person_filled,
        unselectedIcon = R.drawable.ic_person_outline,
        index = 2
    )
}

val tabs = listOf<TabBarItem>(
    TabBarItem.Logs,
    TabBarItem.Rankings,
    TabBarItem.Profile
)

fun getIndexForTab(route: String?): Int {
    return tabs.find { it.title == route }?.index ?: TabBarItem.Logs.index
}