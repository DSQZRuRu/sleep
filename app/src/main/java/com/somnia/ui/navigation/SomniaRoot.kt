package com.somnia.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.somnia.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.somnia.AppContainer
import com.somnia.data.db.GameStateEntity
import com.somnia.domain.game.CompanionStage
import com.somnia.tracking.TrackingState
import com.somnia.ui.history.HistoryScreen
import com.somnia.ui.history.HistoryViewModel
import com.somnia.ui.history.NightDetailScreen
import com.somnia.ui.history.NightDetailViewModel
import com.somnia.ui.home.HomeScreen
import com.somnia.ui.home.HomeViewModel
import com.somnia.ui.journal.JournalScreen
import com.somnia.ui.journal.JournalViewModel
import com.somnia.ui.morning.MorningScreen
import com.somnia.ui.morning.MorningViewModel
import com.somnia.ui.onboarding.OnboardingScreen
import com.somnia.ui.onboarding.OnboardingViewModel
import com.somnia.ui.settings.SettingsScreen
import com.somnia.ui.settings.SettingsViewModel
import com.somnia.ui.theme.SomniaColors
import com.somnia.ui.tips.TipsScreen
import com.somnia.ui.tracking.TrackingScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector?) {
    NIGHT("night", "Nuit", null),
    HISTORY("history", "Historique", Icons.Filled.DateRange),
    JOURNAL("journal", "Carnet", Icons.Filled.Star),
    TIPS("tips", "Conseils", Icons.Filled.Info),
}

/**
 * Racine de l'app : choisit l'écran selon l'état global
 * (profil à créer → nuit en cours → récompenses à révéler → navigation principale).
 */
@Composable
fun SomniaRoot(container: AppContainer) {
    val profile by container.profileRepository.profile.collectAsStateWithLifecycle(null)
    val tracking by TrackingState.status.collectAsStateWithLifecycle()
    val pending by container.sleepRepository.pendingReveal.collectAsStateWithLifecycle(null)
    val game by container.gameRepository.state.collectAsStateWithLifecycle(GameStateEntity())

    val p = profile
    when {
        p == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        !p.onboardingDone -> {
            val vm = containerViewModel { OnboardingViewModel(container.profileRepository) }
            OnboardingScreen(vm, onDone = {})
        }
        tracking.running -> TrackingScreen(tracking, CompanionStage.forCare(game.companionCare))
        pending != null -> {
            val id = pending!!.id
            val vm = containerViewModel(key = "morning-$id") { MorningViewModel(container, id) }
            MorningScreen(vm, onDone = {})
        }
        else -> MainNavigation(container)
    }
}

@Composable
private fun MainNavigation(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == route }

    Scaffold(
        containerColor = SomniaColors.Night,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = SomniaColors.Surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val v = tab.icon
                            if (v != null) Icon(v, null) else Icon(painterResource(R.drawable.ic_moon), null)
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = SomniaColors.SurfaceHigh),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.NIGHT.route, modifier = Modifier.padding(padding)) {
            composable(Tab.NIGHT.route) {
                HomeScreen(
                    containerViewModel { HomeViewModel(container) },
                    onSettings = { nav.navigate("settings") },
                    onOpenTip = { nav.navigate(Tab.TIPS.route) { launchSingleTop = true } },
                )
            }
            composable(Tab.HISTORY.route) {
                HistoryScreen(containerViewModel { HistoryViewModel(container) }, onOpen = { nav.navigate("night/$it") })
            }
            composable(Tab.JOURNAL.route) { JournalScreen(containerViewModel { JournalViewModel(container) }) }
            composable(Tab.TIPS.route) { TipsScreen(container.profileRepository.profile) }
            composable("settings") {
                SettingsScreen(containerViewModel { SettingsViewModel(container) }, onBack = { nav.popBackStack() })
            }
            composable("night/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                NightDetailScreen(containerViewModel(key = "night-$id") { NightDetailViewModel(container, id) }, onBack = { nav.popBackStack() })
            }
        }
    }
}
