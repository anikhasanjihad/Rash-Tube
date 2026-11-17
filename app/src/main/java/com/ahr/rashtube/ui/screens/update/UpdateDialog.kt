package com.ahr.rashtube.ui.screens.update

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahr.rashtube.R
import com.ahr.rashtube.ui.components.shared.RashActionButton

private const val BLUR_RADIUS_PX = 48
private const val DIM_AMOUNT = 0.4f
private const val MAX_HIGHLIGHTS = 3
private const val PERCENT = 100
private val BrandRed = Color(0xFFE60000)

/** Opens the system's install confirmation as soon as the installer has one ready. */
@Composable
internal fun ConfirmationLauncher(viewModel: UpdateViewModel) {
    val context = LocalContext.current
    val confirmation by viewModel.confirmation.collectAsStateWithLifecycle()
    LaunchedEffect(confirmation) {
        if (confirmation != null) viewModel.consumeConfirmation()?.let { runCatching { context.startActivity(it) } }
    }
}

/**
 * The update every launch waits on: a small card over a blurred app, with the version, a few highlights
 * and the one button the current step needs. It cannot be dismissed; the app is usable again once the
 * new version is installed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ForcedUpdateDialog(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    ConfirmationLauncher(viewModel)
    val release = state.release ?: return
    val stage = state.stage
    val action = stage.action()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        BlurBehindWindow()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.padding(24.dp).widthIn(max = 360.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(64.dp).background(BrandRed, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification_logo),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.update_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.update_dialog_version, release.version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    highlights(release.notes).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    (stage as? UpdateStage.Failed)?.let { failed ->
                        Text(
                            text = stringResource(failed.reason.messageRes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    RashActionButton(
                        text = action.label(),
                        onClick = { viewModel.onPrimary { runCatching { context.startActivity(it) } } },
                        enabled = action.enabled,
                        leading = action.icon,
                        progress = (stage as? UpdateStage.Downloading)?.let { downloading -> { downloading.progress ?: 0f } },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    if (stage is UpdateStage.Downloading) {
                        TextButton(onClick = viewModel::cancelDownload) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
        }
    }
}

/** Blurs whatever is behind the dialog (Android 12 and up) and dims it, so the app is still there but out of reach. */
@Composable
private fun BlurBehindWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(DIM_AMOUNT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = BLUR_RADIUS_PX }
        }
    }
}

/** The first few lines of the release notes as short plain sentences. */
private fun highlights(notes: String): List<String> =
    notes
        .lineSequence()
        .map { it.replace("**", "").trim().trimStart('-', '*', '#', '•', ' ') }
        .filter { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }
        .take(MAX_HIGHLIGHTS)
        .toList()
