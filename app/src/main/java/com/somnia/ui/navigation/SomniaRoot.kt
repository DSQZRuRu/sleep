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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.somnia.ui.theme.SomniaColors
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.somnia.ui.game.GameNavBar
import com.somnia.ui.game.NavItem
import com.somnia.ui.tips.TipsScreen
import com.somnia.ui.tracking.TrackingScreen
import androidx.compose.runtime.LaunchedEffect
import com.somnia.data.db.ProfileEntity
import com.somnia.data.repository.NightPlanner
import com.somnia.ui.dreams.DreamJournalScreen
import com.somnia.ui.recap.RecapScreen
import com.somnia.ui.recap.RecapViewModel
import com.somnia.ui.ritual.RitualScreen
import com.somnia.ui.room.RoomScreen
import com.somnia.ui.room.RoomViewModel
import com.somnia.update.UpdateBanner
import com.somnia.update.UpdateViewModel

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

    var showNight by remember { mutableStateOf(false) }
    LaunchedEffect(tracking.running) { if (!tracking.running) showNight = false }
    val p = profile
    when {
        p == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        !p.onboardingDone -> {
            val vm = containerViewModel { OnboardingViewModel(container.profileRepository, container.backupManager, container.appContext) }
            OnboardingScreen(vm, onDone = {})
        }
        // Suivi automatique : l'app reste utilisable ; l'écran de nuit s'affiche sur demande.
        tracking.running && (!tracking.auto || showNight) -> TrackingScreen(tracking, CompanionStage.forCare(game.companionCare))
        pending != null -> {
            val id = pending!!.id
            val vm = containerViewModel(key = "morning-$id") { MorningViewModel(container, id) }
            MorningScreen(vm, onDone = {})
        }
        else -> MainNavigation(container, autoTracking = tracking.running && tracking.auto, onShowNight = { showNight = true })
    }
}

@Composable
private fun MainNavigation(container: AppContainer, autoTracking: Boolean = false, onShowNight: () -> Unit = {}) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == route }
    // Toute navigation vers un onglet (barre ou raccourci) passe par ici, pour garder une pile cohérente.
    val goToTab: (Tab) -> Unit = { tab ->
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    // Ouverture demandée par une notification.
    val pending by PendingNav.target.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        when (pending) {
            PendingNav.RITUAL -> nav.navigate("ritual") { launchSingleTop = true }
            PendingNav.RECAP -> nav.navigate("recap/0") { launchSingleTop = true }
        }
        PendingNav.target.value = null
    }
    // Partagé par le bandeau et les réglages (portée de l'activité).
    val updates = containerViewModel { UpdateViewModel(container.appContext) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            androidx.compose.foundation.layout.Column {
                UpdateBanner(updates)
                if (autoTracking) AutoTrackingBanner(onShowNight)
            }
        },
        bottomBar = {
            if (showBar) GameNavBar(
                Tab.entries.map { tab ->
                    NavItem(
                        label = tab.label,
                        icon = tab.icon?.let { rememberVectorPainter(it) } ?: painterResource(R.drawable.ic_moon),
                        selected = route == tab.route,
                        onClick = { goToTab(tab) },
                    )
                }
            )
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.NIGHT.route, modifier = Modifier.padding(padding)) {
            composable(Tab.NIGHT.route) {
                HomeScreen(
                    containerViewModel { HomeViewModel(container) },
                    onSettings = { nav.navigate("settings") },
                    onOpenTip = { goToTab(Tab.TIPS) },
                    onOpenJournal = { goToTab(Tab.JOURNAL) },
                    onOpenRoom = { nav.navigate("room") },
                    onOpenRitual = { nav.navigate("ritual") },
                )
            }
            composable(Tab.HISTORY.route) {
                HistoryScreen(
                    containerViewModel { HistoryViewModel(container) },
                    onOpen = { nav.navigate("night/$it") },
                    onOpenRecap = { weeks -> nav.navigate("recap/$weeks") },
                    onOpenDreams = { nav.navigate("dreams") },
                )
            }
            composable(Tab.JOURNAL.route) { JournalScreen(containerViewModel { JournalViewModel(container) }) }
            composable(Tab.TIPS.route) { TipsScreen(container.profileRepository.profile, container.sleepRepository.history) }
            composable("dreams") {
                DreamJournalScreen(container.sleepRepository.history, onBack = { nav.popBackStack() },
                    onOpenNight = { nav.navigate("night/$it") })
            }
            composable("room") {
                RoomScreen(containerViewModel { RoomViewModel(container) }, onBack = { nav.popBackStack() },
                    onManageBed = { goToTab(Tab.JOURNAL) })
            }
            composable("ritual") {
                val game by container.gameRepository.state.collectAsStateWithLifecycle(GameStateEntity())
                val profile by container.profileRepository.profile.collectAsStateWithLifecycle(ProfileEntity())
                RitualScreen(
                    CompanionStage.forCare(game.companionCare), container.reminderPrefs,
                    profileWakeAlarm = { NightPlanner.nextWakeAt(profile) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable("recap/{weeks}", arguments = listOf(navArgument("weeks") { type = NavType.IntType })) { entry ->
                val weeks = entry.arguments?.getInt("weeks") ?: 0
                RecapScreen(containerViewModel(key = "recap-$weeks") { RecapViewModel(container, weeks) }, onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(containerViewModel { SettingsViewModel(container) }, updates, onBack = { nav.popBackStack() })
            }
            composable("night/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                NightDetailScreen(containerViewModel(key = "night-$id") { NightDetailViewModel(container, id) }, onBack = { nav.popBackStack() })
            }
        }
    }
}


/** Bandeau du suivi automatique : la nuit commencera quand le téléphone sera lâché. */
@Composable
private fun AutoTrackingBanner(onShowNight: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)
            .background(SomniaColors.SurfaceHigh, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text("☾ Suivi automatique actif", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        Text("La nuit commencera quand vous lâcherez le téléphone. Utilisez-le librement en attendant.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        androidx.compose.foundation.layout.Row {
            androidx.compose.material3.TextButton(onClick = onShowNight) { Text("Écran de nuit") }
            androidx.compose.material3.TextButton(onClick = {
                context.startService(com.somnia.tracking.SleepTrackingService.cancelIntent(context))
            }) { Text("Pas ce soir") }
        }
    }
}
