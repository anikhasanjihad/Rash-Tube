package com.ahr.rashtube.ui.components.shared.card

import android.view.LayoutInflater
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ahr.rashtube.R
import com.ahr.rashtube.player.preview.FeedPreviewController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The muted preview over a card's thumbnail while [videoId] is the one [FeedPreviewController] is
 * playing. The video fades in once its first frame is up, so the thumbnail never blinks to black.
 * The preview ends when the card leaves the screen and when the app goes to the background.
 */
@Composable
internal fun BoxScope.VideoCardPreview(
    videoId: String,
    autoPlay: Boolean,
) {
    val activeId by FeedPreviewController.activeVideoId.collectAsState()
    val player by FeedPreviewController.player.collectAsState()
    val firstFrameShown by FeedPreviewController.firstFrameShown.collectAsState()
    val alpha by animateFloatAsState(targetValue = if (firstFrameShown) 1f else 0f, label = "feed_preview_alpha")

    if (autoPlay) {
        AutoPreviewOnRest(videoId)
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(videoId, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) FeedPreviewController.stop(videoId)
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            FeedPreviewController.stop(videoId)
        }
    }

    val exo = player
    if (activeId == videoId && exo != null) {
        AndroidView(
            factory = { context ->
                // The texture-view layout, so the video fades and clips with the rounded thumbnail.
                (LayoutInflater.from(context).inflate(R.layout.video_player_view, null) as PlayerView).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { it.player = exo },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
        )
    }
}

/**
 * Starts the card's preview once it has rested near the middle of the window for [DWELL_MS], and
 * stops it when it scrolls out of that band. Every position change restarts the wait, so nothing
 * plays while the feed is moving.
 */
@Composable
private fun BoxScope.AutoPreviewOnRest(videoId: String) {
    val context = LocalContext.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    var centerY by remember { mutableFloatStateOf(-1f) }
    Spacer(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                centerY = coordinates.positionInWindow().y + coordinates.size.height / 2f
            },
    )
    LaunchedEffect(videoId, windowHeight) {
        snapshotFlow { centerY }.collectLatest { y ->
            val inBand = y >= windowHeight * BAND_START && y <= windowHeight * BAND_END
            if (inBand) {
                delay(DWELL_MS)
                FeedPreviewController.startIfIdle(context, videoId)
            } else {
                FeedPreviewController.stop(videoId)
            }
        }
    }
}

private const val DWELL_MS = 1_500L
private const val BAND_START = 0.3f
private const val BAND_END = 0.7f
