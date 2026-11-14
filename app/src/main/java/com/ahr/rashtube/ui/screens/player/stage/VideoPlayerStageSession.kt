package com.ahr.rashtube.ui.screens.player.stage

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Stable
import com.ahr.rashtube.data.model.Video
import com.ahr.rashtube.player.state.EnhancedPlayerState
import com.ahr.rashtube.ui.components.videoplayer.PlayerDraggableState
import com.ahr.rashtube.ui.screens.player.VideoPlayerViewModel
import com.ahr.rashtube.ui.screens.player.effects.AudioSystemInfo
import com.ahr.rashtube.ui.screens.player.effects.PipPreferences
import com.ahr.rashtube.ui.screens.player.state.PlayerCommentsUiState
import com.ahr.rashtube.ui.screens.player.state.PlayerScreenState
import com.ahr.rashtube.ui.screens.player.state.VideoPlayerPreferencesState
import com.ahr.rashtube.ui.screens.player.state.VideoPlayerUiState
import kotlinx.coroutines.CoroutineScope

/**
 * Everything the player's stage surfaces read about the session they are drawing. Built fresh by
 * the host on each composition, so the values it carries are exactly the ones the host itself
 * observed that frame.
 */
@Stable
internal class VideoPlayerStageSession(
    val video: Video,
    val context: Context,
    val activity: ComponentActivity,
    val scope: CoroutineScope,
    val screenState: PlayerScreenState,
    val playerState: EnhancedPlayerState,
    val uiState: VideoPlayerUiState,
    val commentsUiState: PlayerCommentsUiState,
    val viewModel: VideoPlayerViewModel,
    val sheetState: PlayerDraggableState,
    val prefs: VideoPlayerPreferencesState,
    val audioSystemInfo: AudioSystemInfo,
    val pipPreferences: PipPreferences,
    /** Whether the window shows the fullscreen layout yet, which trails the request while it rotates. */
    val isFullscreenShown: Boolean,
    val ambient: PlayerAmbient,
)
