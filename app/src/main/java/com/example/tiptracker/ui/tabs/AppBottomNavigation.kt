package com.example.tiptracker.ui.tabs

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppBottomNavigation(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        tabs.forEach { tab ->
            val tabSelected = currentDestination?.hierarchy?.any {
                it.route == tab.title
            } == true

            NavigationBarItem(
                icon = {
                    TabBarIcon(
                        item = tab,
                        isSelected = tabSelected
                    )
                },
                label = {
                    Text(text = tab.title)
                },
                selected = tabSelected,
                onClick = {
                    navController.navigate(tab.title) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun TabBarIcon(
    item: TabBarItem,
    isSelected: Boolean
) {
    BadgedBox(badge = { TabBarBadge(item.badgeCount) }) {
        Icon(
            painter = painterResource(id = if (isSelected) item.selectedIcon else item.unselectedIcon),
            contentDescription = item.title
        )
    }
}

@Composable
fun TabBarBadge(count: Int? = null) {
    if (count != null) {
        Badge {
            Text(text = count.toString())
        }
    }
}