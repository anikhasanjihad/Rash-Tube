package com.ahr.rashtube.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.ahr.rashtube.data.subscriptions.refreshSubscriptionsAtStartup
import com.ahr.rashtube.player.DeepFlowManager
import com.ahr.rashtube.player.EnhancedMusicPlayerManager
import com.ahr.rashtube.player.SleepTimerManager
import com.ahr.rashtube.service.MusicServiceState
import com.ahr.rashtube.ui.components.layout.navigation.RashTab
import com.ahr.rashtube.ui.screens.music.sharedMusicPlayerViewModel
import kotlinx.coroutines.flow.collectLatest

/** App-wide effects with no UI of their own: preference propagation and the global snackbar feeds. */
@Composable
internal fun RashAppSideEffects(
    snackbarHostState: SnackbarHostState,
    sleepTimerCloseAppOnExpiry: Boolean,
    subscriptionRefreshOnStartup: Boolean,
) {
    val context = LocalContext.current

    // Music the system media controls resumed plays before any music screen exists; the player
    // manager starts with its ViewModel, so start it now for the mini player and queue to show.
    if (remember { MusicServiceState.isRunning }) sharedMusicPlayerViewModel()

    LaunchedEffect(sleepTimerCloseAppOnExpiry) {
        SleepTimerManager.updatePreferredCloseAppOnExpiry(sleepTimerCloseAppOnExpiry)
    }

    LaunchedEffect(subscriptionRefreshOnStartup) {
        if (subscriptionRefreshOnStartup) {
            refreshSubscriptionsAtStartup(context.applicationContext)
        }
    }

    LaunchedEffect(snackbarHostState) {
        DeepFlowManager.messages.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(snackbarHostState) {
        EnhancedMusicPlayerManager.playbackWarnings.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
        }
    }
}

/** Detail screens keep the tab they were opened from, so this holds the last tab root navigated to. */
@Composable
internal fun rememberSelectedFlowTab(
    navController: NavHostController,
    defaultTab: RashTab,
    visibleTabs: () -> List<RashTab>,
): State<RashTab> {
    val selectedTab = remember { mutableStateOf(defaultTab) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val tab = entry.flowTab()
            if (tab != null && tab in visibleTabs()) selectedTab.value = tab
        }
    }
    return selectedTab
}

/** Follows a changed default tab, and leaves Home for the default tab once the user hides Home. */
@Composable
internal fun RashStartTabEffects(
    navController: NavHostController,
    currentRoute: MutableState<String>,
    defaultTab: RashTab,
    isHomeNavigationEnabled: Boolean,
    needsOnboarding: Boolean?,
) {
    LaunchedEffect(defaultTab) {
        currentRoute.value = defaultTab.route
    }

    LaunchedEffect(isHomeNavigationEnabled, currentRoute.value, defaultTab, needsOnboarding) {
        if (needsOnboarding == false && !isHomeNavigationEnabled && currentRoute.value == "home") {
            currentRoute.value = defaultTab.route
            navController.navigate(defaultTab.route) {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
