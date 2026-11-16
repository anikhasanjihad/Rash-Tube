package com.ahr.rashtube.ui.screens.update

import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ahr.rashtube.BuildConfig
import com.ahr.rashtube.R
import com.ahr.rashtube.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What changed in the version that was just installed, shown once after an update. */
data class WhatsNew(
    val version: String,
    val notes: String,
)

@HiltViewModel
class WhatsNewViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val updates: UpdateRepository,
    ) : ViewModel() {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        private val _whatsNew = MutableStateFlow<WhatsNew?>(null)
        val whatsNew: StateFlow<WhatsNew?> = _whatsNew.asStateFlow()
        private var started = false

        /** Looks once per launch; a fresh install has nothing to announce, only a changed version does. */
        fun check() {
            if (started || BuildConfig.DEBUG) return
            started = true
            val current = BuildConfig.VERSION_NAME
            val seen = prefs.getString(KEY_SEEN, null)
            if (seen == current) return
            if (seen == null) {
                markSeen(current)
                return
            }
            viewModelScope.launch {
                val notes = updates.notesFor(current)
                if (notes == null) markSeen(current) else _whatsNew.value = WhatsNew(current, notes)
            }
        }

        fun dismiss() {
            _whatsNew.value?.let { markSeen(it.version) }
            _whatsNew.value = null
        }

        private fun markSeen(version: String) {
            prefs.edit().putString(KEY_SEEN, version).apply()
        }

        private companion object {
            const val PREFS = "rash_whats_new"
            const val KEY_SEEN = "seen_version"
        }
    }

/** Shows the release notes of the running version once, right after an update has been installed. */
@Composable
fun WhatsNewGate(needsOnboarding: Boolean?) {
    val viewModel: WhatsNewViewModel = hiltViewModel()
    androidx.compose.runtime.LaunchedEffect(needsOnboarding) { if (needsOnboarding == false) viewModel.check() }
    val whatsNew by viewModel.whatsNew.collectAsStateWithLifecycle()
    whatsNew?.let { info ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_whats_new_title, info.version)) },
            text = { Text(info.notes, modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_whats_new_dismiss)) } },
        )
    }
}
