package com.ahr.rashtube.player.preview

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.ahr.rashtube.innertube.models.response.PlayerResponse
import com.ahr.rashtube.player.cache.SharedPlayerCacheProvider
import com.ahr.rashtube.player.datasource.YouTubeHttpDataSource
import com.ahr.rashtube.player.shorts.ShortsPlayerFactory
import com.ahr.rashtube.player.stream.InnerTubeVideoStreamExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The muted, video-only preview a feed card plays on long press, the way YouTube previews a
 * thumbnail in the feed.
 *
 * One player at a time: starting a preview on another card stops the first. The player has its
 * audio track disabled and never requests audio focus, so music or a podcast already playing is
 * left alone, and it reads a low-resolution stream so a preview stays cheap on data.
 */
@OptIn(UnstableApi::class)
object FeedPreviewController {
    private const val TAG = "FeedPreview"
    private const val EXTRACT_TIMEOUT_MS = 15_000L
    private const val MAX_PREVIEW_MS = 30_000L
    private const val TARGET_HEIGHT = 360

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activeVideoId = MutableStateFlow<String?>(null)

    /** The video whose preview is starting or playing, or null. */
    val activeVideoId: StateFlow<String?> = _activeVideoId.asStateFlow()

    private val _player = MutableStateFlow<ExoPlayer?>(null)

    /** The player for [activeVideoId] once its stream is resolved. */
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _firstFrameShown = MutableStateFlow(false)

    /** True once the first frame is on screen, so the card can fade the video in over the thumbnail. */
    val firstFrameShown: StateFlow<Boolean> = _firstFrameShown.asStateFlow()

    private var job: Job? = null

    /** Starts a muted preview of [videoId], replacing any other. Main thread. */
    fun start(
        context: Context,
        videoId: String,
    ) {
        if (_activeVideoId.value == videoId) return
        stop()
        _activeVideoId.value = videoId
        val appContext = context.applicationContext
        job =
            scope.launch {
                val result =
                    withTimeoutOrNull(EXTRACT_TIMEOUT_MS) {
                        runCatching { InnerTubeVideoStreamExtractor.extract(videoId) }.getOrNull()
                    }
                if (_activeVideoId.value != videoId) return@launch
                // A live broadcast has no progressive stream to loop, so its card keeps the thumbnail.
                val url = result?.takeUnless { it.isLive }?.videoFormats?.let(::pickPreviewUrl)
                if (url == null) {
                    Log.d(TAG, "No preview stream for $videoId")
                    stop(videoId)
                    return@launch
                }
                val exo = buildPlayer(appContext)
                exo.setMediaSource(
                    ProgressiveMediaSource
                        .Factory(dataSourceFactory(appContext))
                        .createMediaSource(MediaItem.fromUri(url)),
                )
                exo.prepare()
                exo.playWhenReady = true
                _player.value = exo
                delay(MAX_PREVIEW_MS)
                stop(videoId)
            }
    }

    /** Stops the preview. With [videoId], only if that video's preview is the one running. */
    fun stop(videoId: String? = null) {
        if (videoId != null && _activeVideoId.value != videoId) return
        job?.cancel()
        job = null
        _activeVideoId.value = null
        _firstFrameShown.value = false
        _player.value?.release()
        _player.value = null
    }

    private fun buildPlayer(context: Context): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                // No setAudioAttributes(…, handleAudioFocus = true): a silent preview must not pause music.
                trackSelectionParameters =
                    trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .setMaxVideoSize(Int.MAX_VALUE, TARGET_HEIGHT)
                        .build()
                addListener(
                    object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            _firstFrameShown.value = true
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.w(TAG, "Preview failed: ${error.errorCodeName}")
                            stop()
                        }
                    },
                )
            }

    private fun dataSourceFactory(context: Context): DataSource.Factory =
        SharedPlayerCacheProvider.existing()?.let { ShortsPlayerFactory.cachedDataSourceFactory(context, it) }
            ?: DefaultDataSource.Factory(context, YouTubeHttpDataSource.Factory())

    /**
     * The tallest MP4 at or under [TARGET_HEIGHT], else the shortest one there is. MP4 because every
     * device decodes it in hardware; the preview is silent, so video-only streams are fine.
     */
    private fun pickPreviewUrl(formats: List<PlayerResponse.StreamingData.Format>): String? {
        val candidates =
            formats.filter { it.url != null && it.height != null && it.mimeType.startsWith("video/mp4") }
        val best =
            candidates.filter { (it.height ?: 0) <= TARGET_HEIGHT }.maxByOrNull { it.height ?: 0 }
                ?: candidates.minByOrNull { it.height ?: Int.MAX_VALUE }
        return best?.url
    }
}
