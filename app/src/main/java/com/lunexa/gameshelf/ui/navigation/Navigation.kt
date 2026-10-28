package com.lunexa.gameshelf.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.unit.dp
import com.lunexa.gameshelf.ui.detail.GameDetailScreen
import com.lunexa.gameshelf.ui.edit.EditGameScreen
import com.lunexa.gameshelf.ui.home.HomeScreen
import com.lunexa.gameshelf.ui.library.LibraryScreen
import com.lunexa.gameshelf.ui.lists.ListsScreen
import com.lunexa.gameshelf.ui.profile.ProfileScreen
import com.lunexa.gameshelf.ui.settings.SettingsScreen
import com.lunexa.gameshelf.ui.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library?q={q}"
    const val STATS = "stats"
    const val LISTS = "lists?tab={tab}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val DETAIL = "game/{id}"
    const val EDIT = "edit?id={id}&wish={wish}"

    fun library(query: String? = null) = if (query.isNullOrBlank()) "library" else "library?q=${Uri.encode(query)}"
    fun lists(tab: Int = 0) = "lists?tab=$tab"
    fun detail(id: Long) = "game/$id"
    fun edit(id: Long? = null, wish: Boolean = false) = "edit?id=${id ?: -1L}&wish=$wish"
}

private data class TabItem(
    val pattern: String,
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val tabs = listOf(
    TabItem(Routes.HOME, Routes.HOME, "Главная", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem(Routes.LIBRARY, "library", "Библиотека", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    TabItem(Routes.STATS, Routes.STATS, "Статистика", Icons.Outlined.BarChart, Icons.Filled.BarChart),
    TabItem(Routes.LISTS, "lists", "Списки", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
    TabItem(Routes.PROFILE, Routes.PROFILE, "Профиль", Icons.Outlined.Person, Icons.Filled.Person)
)

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun GameShelfRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentPattern = backStack?.destination?.route
    val showBar = tabs.any { it.pattern == currentPattern }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = tab.pattern == currentPattern
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (!selected) nav.navigateTab(tab.route) },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, tab.title) },
                            label = { Text(tab.title, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 14 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(220)) { it / 14 } }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenGame = { nav.navigate(Routes.detail(it)) },
                    onAddGame = { nav.navigate(Routes.edit()) },
                    onSearch = { q ->
                        nav.navigate(Routes.library(q)) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenLibrary = { nav.navigateTab("library") },
                    onOpenWishlist = {
                        nav.navigate(Routes.lists(1)) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenProfile = { nav.navigateTab(Routes.PROFILE) }
                )
            }
            composable(
                Routes.LIBRARY,
                arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) {
                LibraryScreen(
                    onOpenGame = { nav.navigate(Routes.detail(it)) },
                    onAddGame = { nav.navigate(Routes.edit()) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(onOpenGame = { nav.navigate(Routes.detail(it)) })
            }
            composable(
                Routes.LISTS,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) {
                ListsScreen(
                    onOpenGame = { nav.navigate(Routes.detail(it)) },
                    onAddWish = { nav.navigate(Routes.edit(wish = true)) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenGame = { nav.navigate(Routes.detail(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                GameDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.edit(id)) }
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("wish") { type = NavType.BoolType; defaultValue = false }
                )
            ) {
                EditGameScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { id, isNew ->
                        if (isNew) {
                            nav.navigate(Routes.detail(id)) {
                                popUpTo(Routes.EDIT) { inclusive = true }
                            }
                        } else {
                            nav.popBackStack()
                        }
                    },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
        }
    }
}
