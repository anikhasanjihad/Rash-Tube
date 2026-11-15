package com.ahr.rashtube.ui.components.musicplayer.full

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ahr.rashtube.R
import com.ahr.rashtube.ui.components.shared.ArtworkThumbnail
import com.ahr.rashtube.ui.screens.music.MusicPlayerUiState
import com.ahr.rashtube.ui.screens.music.MusicPlayerViewModel
import kotlin.math.abs

private const val SWIPE_SKIP_THRESHOLD_PX = 150f

/** The context may be wrapped (locale, theme), so walk down to the activity that owns the window. */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * Ambient Mode: the song's cover beside its synced lyrics, full screen and landscape, with the screen
 * kept awake. Double-tap the cover to play or pause; swipe sideways to skip. It is a dialog over the
 * player, so closing it returns to exactly where the player was.
 */
@Composable
internal fun AmbientModeOverlay(
    uiState: MusicPlayerUiState,
    viewModel: MusicPlayerViewModel,
    backdropColor: Color,
    accentColor: Color,
    artworkUrl: String,
    positionState: State<Long>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        val activity = LocalContext.current.findActivity()

        DisposableEffect(activity, dialogWindow) {
            val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            dialogWindow?.let { window ->
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {
                activity?.requestedOrientation = previousOrientation
                dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        var dragX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backdropColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = 0f },
                            onDragEnd = {
                                if (abs(dragX) > SWIPE_SKIP_THRESHOLD_PX) {
                                    if (dragX > 0) viewModel.skipToPrevious() else viewModel.skipToNext()
                                }
                                dragX = 0f
                            },
                            onDragCancel = { dragX = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragX += amount
                            },
                        )
                    },
        ) {
            Row(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    ArtworkThumbnail(
                        thumbnailUrl = artworkUrl,
                        modifier =
                            Modifier
                                .fillMaxHeight(0.85f)
                                .aspectRatio(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = { viewModel.togglePlayPause() })
                                },
                        size = androidx.compose.ui.unit.Dp.Unspecified,
                        shape = MaterialTheme.shapes.large,
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    NowPlayingLyricsPane(
                        uiState = uiState,
                        viewModel = viewModel,
                        accentColor = accentColor,
                        backdropColor = backdropColor,
                        positionState = positionState,
                        active = true,
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}
