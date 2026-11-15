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
import androidx.lifecycle.ViewModel
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
        private var checked = false
        private val _openUpdate = MutableStateFlow(false)
        val openUpdate: StateFlow<Boolean> = _openUpdate.asStateFlow()

        fun check() {
            if (checked) return
            checked = true
            if (BuildConfig.DEBUG || !BuildConfig.UPDATER_ENABLED) {
                prompts.updateCheckFinished(shownUpdate = false)
                return
            }
            viewModelScope.launch {
                val release = updates.requiredRelease()
                prompts.updateCheckFinished(shownUpdate = release != null)
                if (release != null) _openUpdate.value = true
            }
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
