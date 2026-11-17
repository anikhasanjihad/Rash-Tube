package com.ahr.rashtube.ui.screens.home.preview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ahr.rashtube.R
import com.ahr.rashtube.data.local.PlayerPreferences
import com.ahr.rashtube.data.shorts.ShortsStreamResolver
import com.ahr.rashtube.network.ProxyAwareClient
import com.ahr.rashtube.ui.screens.home.HomeFeedRow
import com.ahr.rashtube.utils.NetworkState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs

private const val SETTLE_MS = 700L
private const val HOLD_MS = 300L
private const val PREVIEW_HEIGHT = 360

/**
 * Which feed card is playing its muted preview. A video held under the finger wins over the one the
 * feed settled on, and the preview stops the moment the finger lifts or the feed scrolls.
 */
@Stable
class HomeFeedPreviewState {
    var autoId by mutableStateOf<String?>(null)
    var holdId by mutableStateOf<String?>(null)
    val activeId: String? get() = holdId ?: autoId
}

/** Provided by the Home feed only, so every other list keeps its plain cards. */
val LocalHomeFeedPreview = staticCompositionLocalOf<HomeFeedPreviewState?> { null }

@HiltViewModel
class HomeFeedPreviewViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val resolver: ShortsStreamResolver,
        preferences: PlayerPreferences,
    ) : ViewModel() {
        /** On unless switched off, and held back on mobile data while Data saver is on. */
        val enabled: StateFlow<Boolean> =
            combine(preferences.homeFeedPreviewEnabled, preferences.dataSaverEnabled) { on, saver ->
                on && !(saver && !NetworkState.isOnWifi(context))
            }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val dataSourceFactory: DataSource.Factory = OkHttpDataSource.Factory(ProxyAwareClient().get())

        suspend fun urlFor(videoId: String): String? = resolver.resolve(videoId, PREVIEW_HEIGHT, "")?.videoUrl
    }

/**
 * The state for one Home grid. While the feed rests, the fully visible video nearest the middle of the
 * screen becomes the previewed one after a short settle; scrolling or leaving the screen stops it.
 */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberHomeFeedPreviewState(
    gridState: LazyGridState,
    rows: List<HomeFeedRow>,
    enabled: Boolean,
): HomeFeedPreviewState {
    val state = remember { HomeFeedPreviewState() }
    val currentRows by rememberUpdatedState(rows)
    val resumed = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(gridState, enabled, resumed) {
        if (!enabled || !resumed) {
            state.autoId = null
            state.holdId = null
            return@LaunchedEffect
        }
        snapshotFlow { if (gridState.isScrollInProgress) null else centeredVideoId(gridState, currentRows) }
            .distinctUntilChanged()
            .debounce { if (it == null) 0L else SETTLE_MS }
            .collect { state.autoId = it }
    }
    return state
}

private fun centeredVideoId(
    gridState: LazyGridState,
    rows: List<HomeFeedRow>,
): String? {
    val info = gridState.layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    val videos = info.visibleItemsInfo.filter { rows.getOrNull(it.index) is HomeFeedRow.VideoCard }
    val fullyVisible =
        videos.filter { it.offset.y >= info.viewportStartOffset && it.offset.y + it.size.height <= info.viewportEndOffset }
    val chosen = fullyVisible.ifEmpty { videos }.minByOrNull { abs(it.offset.y + it.size.height / 2 - center) } ?: return null
    return (rows[chosen.index] as HomeFeedRow.VideoCard).video.id
}

/** Hold a card for a moment and its preview plays until the finger lifts; the lift does not open the video. */
@Composable
fun Modifier.homeFeedHoldPreview(videoId: String): Modifier {
    val preview = LocalHomeFeedPreview.current ?: return this
    return pointerInput(videoId, preview) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val held =
                try {
                    withTimeout(HOLD_MS) {
                        waitForUpOrCancellation()
                        false
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    true
                }
            if (!held) return@awaitEachGesture
            preview.holdId = videoId
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.isConsumed }) break
                    val lifted = event.changes.none { it.pressed }
                    // Swallowed so neither the tap nor the card's long-press menu fires after a preview.
                    event.changes.forEach { it.consume() }
                    if (lifted) break
                }
            } finally {
                preview.holdId = null
            }
        }
    }
}

/** Plays the muted preview over a card's thumbnail while this card is the active one. */
@Composable
fun HomeFeedPreviewOverlay(videoId: String) {
    val state = LocalHomeFeedPreview.current ?: return
    if (state.activeId != videoId) return
    val viewModel: HomeFeedPreviewViewModel = hiltViewModel()
    var url by remember(videoId) { mutableStateOf<String?>(null) }
    var failed by remember(videoId) { mutableStateOf(false) }
    LaunchedEffect(videoId) { url = runCatching { viewModel.urlFor(videoId) }.getOrNull() }
    val streamUrl = url
    if (streamUrl == null || failed) return
    PreviewPlayer(url = streamUrl, dataSourceFactory = viewModel.dataSourceFactory, onFailed = { failed = true })
}

@OptIn(UnstableApi::class)
@Composable
private fun PreviewPlayer(
    url: String,
    dataSourceFactory: DataSource.Factory,
    onFailed: () -> Unit,
) {
    val context = LocalContext.current
    val reportFailure by rememberUpdatedState(onFailed)
    var hasFrame by remember(url) { mutableStateOf(false) }
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
    DisposableEffect(player, url) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    hasFrame = true
                }

                override fun onPlayerError(error: PlaybackException) = reportFailure()
            }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
        onDispose { player.removeListener(listener) }
    }
    val alpha by animateFloatAsState(targetValue = if (hasFrame) 1f else 0f, label = "homeFeedPreviewAlpha")
    AndroidView(
        factory = { viewContext ->
            (LayoutInflater.from(viewContext).inflate(R.layout.video_player_view, null) as PlayerView).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                subtitleView?.visibility = View.GONE
                this.player = player
            }
        },
        onRelease = { it.player = null },
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha },
    )
}
