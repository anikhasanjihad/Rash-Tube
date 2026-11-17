package com.ahr.rashtube.ui.components.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ahr.rashtube.R

/**
 * A player for decoration, not for watching: silent, looping, never taking audio focus. Home feed
 * previews and a song's motion artwork both use it, and it is released when it leaves composition.
 */
@OptIn(UnstableApi::class)
@Composable
fun rememberMutedLoopPlayer(dataSourceFactory: DataSource.Factory): ExoPlayer {
    val context = LocalContext.current
    val player =
        remember(dataSourceFactory) {
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                    trackSelectionParameters =
                        trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                            .build()
                }
        }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * The picture of a [rememberMutedLoopPlayer], filling its box and cropped to cover it. A TextureView,
 * unlike a SurfaceView, fades and clips with whatever surrounds it.
 */
@OptIn(UnstableApi::class)
@Composable
fun MutedPlayerView(
    player: ExoPlayer,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { viewContext ->
            (LayoutInflater.from(viewContext).inflate(R.layout.video_player_view, null) as PlayerView).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setKeepContentOnPlayerReset(true)
                artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_OFF
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                subtitleView?.visibility = View.GONE
                this.player = player
            }
        },
        onRelease = { it.player = null },
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha },
    )
}
