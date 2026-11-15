package com.ahr.rashtube.ui.screens.update

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ahr.rashtube.BuildConfig
import com.ahr.rashtube.data.update.UpdateRepository
import com.ahr.rashtube.ui.startup.LaunchPrompts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Runs the launch update check once per activity and asks for the update page when it finds one. */
@HiltViewModel
class UpdateLaunchViewModel
    @Inject
    constructor(
        private val updates: UpdateRepository,
        private val prompts: LaunchPrompts,
    ) : ViewModel() {
        private var lastCheckMs = 0L
        private var promptsNotified = false
        private val _openUpdate = MutableStateFlow(false)
        val openUpdate: StateFlow<Boolean> = _openUpdate.asStateFlow()

        /** Asks GitHub for a newer release; repeats each time the app comes forward, at most every few minutes. */
        fun check() {
            val now = SystemClock.elapsedRealtime()
            if (_openUpdate.value || (lastCheckMs != 0L && now - lastCheckMs < RECHECK_MS)) return
            lastCheckMs = now
            if (BuildConfig.DEBUG || !BuildConfig.UPDATER_ENABLED) {
                notifyPrompts(shownUpdate = false)
                return
            }
            viewModelScope.launch {
                val release = updates.requiredRelease()
                notifyPrompts(shownUpdate = release != null)
                if (release != null) _openUpdate.value = true
            }
        }

        private fun notifyPrompts(shownUpdate: Boolean) {
            if (promptsNotified) return
            promptsNotified = true
            prompts.updateCheckFinished(shownUpdate = shownUpdate)
        }

        private companion object {
            const val RECHECK_MS = 5 * 60 * 1000L
        }
    }

/**
 * Mandatory update gate. Once onboarding is done it asks GitHub for a newer release; when there is one,
 * a full-screen update page covers the whole app. It has no close button, no skip and swallows Back, so
 * the app can only be used again after the update is installed (the app restarts on a new version).
 */
@Composable
fun ForcedUpdateGate(needsOnboarding: Boolean?) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    val viewModel: UpdateLaunchViewModel = hiltViewModel(activity)
    val updateRequired by viewModel.openUpdate.collectAsStateWithLifecycle()
    LaunchedEffect(needsOnboarding) { if (needsOnboarding == false) viewModel.check() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (needsOnboarding == false) viewModel.check() }
    if (updateRequired) {
        BackHandler(enabled = true) {}
        Surface(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {},
            color = MaterialTheme.colorScheme.background,
        ) {
            UpdateScreen(onClose = {}, forced = true)
        }
    }
}
