package com.ayuno.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayuno.app.R
import com.ayuno.app.ui.theme.Dimens

/** Pestañas de la navegación global (`specs/UX-UI/README.md`). */
enum class TopLevelDestination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    TODAY("today", R.string.tab_today, Icons.Filled.DateRange),
    HISTORY("history", R.string.tab_history, Icons.AutoMirrored.Filled.List),
    MORE("more", R.string.tab_more, Icons.Filled.Menu),
}

@Composable
fun AyunoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val label = stringResource(destination.label)
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                composable(destination.route) { PendingSection(stringResource(destination.label)) }
            }
        }
    }
}

/** Marcador temporal de las pestañas hasta que sus historias se construyan. */
@Composable
private fun PendingSection(title: String) {
    Box(Modifier.fillMaxSize().padding(Dimens.screenPadding), contentAlignment = Alignment.Center) {
        Text(
            text = "$title\n\n${stringResource(R.string.section_pending)}",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
