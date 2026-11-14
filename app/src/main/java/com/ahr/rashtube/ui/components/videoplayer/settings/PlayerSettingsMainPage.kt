package com.ahr.rashtube.ui.components.videoplayer.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.ahr.rashtube.R
import com.ahr.rashtube.player.EnhancedPlayerState
import com.ahr.rashtube.ui.components.equalizer.equalizerSummary
import com.ahr.rashtube.ui.components.shared.RashNavRow
import com.ahr.rashtube.ui.components.shared.RashRowGroup
import com.ahr.rashtube.ui.components.shared.RashSectionHeader
import com.ahr.rashtube.ui.components.shared.RashSwitchRow
import com.ahr.rashtube.ui.components.shared.flowRowGroupShape
import com.ahr.rashtube.ui.components.shared.playbackSpeedLabel
import com.ahr.rashtube.utils.formatDurationMillis

private const val VIDEO_ROWS = 1
private const val PLAYBACK_ROWS = 3
private const val AUDIO_ROWS = 1
private const val CAPTION_ROWS = 2
private const val OVERLAY_ROWS = 3
private const val EFFECT_ROWS = 3
private const val DISPLAY_ROWS = 2

@Composable
internal fun PlayerSettingsMainPage(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    subtitlesEnabled: Boolean,
    ambientModeEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onShowSubtitleStyle: () -> Unit,
    onCastClick: () -> Unit,
    onPipClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit,
    onVolumeNormalizationToggle: (Boolean) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    ambientGlowEnabled: Boolean? = null,
    onAmbientGlowToggle: (Boolean) -> Unit = {},
    sponsorBlockSegmentCount: Int = 0,
    sponsorBlockOffForVideo: Boolean = false,
    onSponsorBlockToggle: (off: Boolean) -> Unit = {},
    notePositionMs: Long? = null,
    onAddNote: () -> Unit = {},
    onStatsForNerds: () -> Unit = {},
    sphericalViewEnabled: Boolean? = null,
    onSphericalViewToggle: (Boolean) -> Unit = {},
    sphericalMotionEnabled: Boolean? = null,
    onSphericalMotionToggle: (Boolean) -> Unit = {},
    sphericalDragSpeed: Float? = null,
) {
    val playbackRows = if (sponsorBlockSegmentCount > 0) PLAYBACK_ROWS + 1 else PLAYBACK_ROWS
    val qualityResolution =
        qualitySummaryResolution(
            currentQuality = playerState.currentQuality,
            currentQualityKey = playerState.currentQualityKey,
            effectiveQuality = playerState.effectiveQuality,
            options = playerState.availableQualities,
        )
    RashSectionHeader(stringResource(R.string.video))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.HighQuality,
            title = stringResource(R.string.quality),
            trailingText =
                when {
                    qualityResolution == null -> stringResource(R.string.quality_auto)
                    playerState.currentQuality == 0 -> stringResource(R.string.quality_auto_with_resolution, qualityResolution)
                    else -> qualityResolution
                },
            shape = flowRowGroupShape(0, VIDEO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Quality) },
        )
    }

    RashSectionHeader(stringResource(R.string.playback_header))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.Speed,
            title = stringResource(R.string.playback_speed),
            trailingText = playbackSpeedLabel(playerState.playbackSpeed),
            shape = flowRowGroupShape(0, playbackRows),
            onClick = { onNavigateToPage(PlayerSettingsPage.Speed) },
        )
        RashSwitchRow(
            leadingIcon = Icons.Rounded.Repeat,
            title = stringResource(R.string.loop_video),
            checked = playerState.isLooping,
            shape = flowRowGroupShape(1, playbackRows),
            onCheckedChange = onLoopToggle,
        )
        RashSwitchRow(
            leadingIcon = Icons.Filled.SkipNext,
            title = stringResource(R.string.autoplay_next),
            checked = autoplayEnabled,
            enabled = !playerState.isLooping,
            shape = flowRowGroupShape(2, playbackRows),
            onCheckedChange = onAutoplayToggle,
        )
        if (sponsorBlockSegmentCount > 0) {
            RashSwitchRow(
                leadingIcon = ImageVector.vectorResource(R.drawable.ic_block),
                title = stringResource(R.string.player_settings_sponsorblock_video),
                supportingText =
                    pluralStringResource(R.plurals.sb_video_segments, sponsorBlockSegmentCount, sponsorBlockSegmentCount),
                checked = !sponsorBlockOffForVideo,
                shape = flowRowGroupShape(3, playbackRows),
                onCheckedChange = { on -> onSponsorBlockToggle(!on) },
            )
        }
    }

    if (notePositionMs != null) {
        RashSectionHeader(stringResource(R.string.note_title))
        RashRowGroup {
            RashNavRow(
                leadingIcon = Icons.Outlined.MoreTime,
                title = stringResource(R.string.note_at_time, formatDurationMillis(notePositionMs)),
                shape = flowRowGroupShape(0, 1),
                onClick = onAddNote,
            )
        }
    }

    RashSectionHeader(stringResource(R.string.audio_settings_title))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.AudioFile,
            title = stringResource(R.string.audio_track),
            trailingText =
                audioTrackDisplayLabel(
                    playerState.availableAudioTracks.getOrNull(playerState.currentAudioTrack),
                    playerState.currentAudioTrack,
                ),
            shape = flowRowGroupShape(0, AUDIO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Audio) },
        )
    }

    RashSectionHeader(stringResource(R.string.captions))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.Subtitles,
            title = stringResource(R.string.filter_subtitles),
            trailingText =
                if (subtitlesEnabled) stringResource(R.string.on) else stringResource(R.string.off),
            shape = flowRowGroupShape(0, CAPTION_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Subtitles) },
        )
        RashNavRow(
            leadingIcon = Icons.Filled.Tune,
            title = stringResource(R.string.subtitle_style),
            shape = flowRowGroupShape(1, CAPTION_ROWS),
            onClick = onShowSubtitleStyle,
        )
    }

    RashSectionHeader(stringResource(R.string.player_settings_overlay_controls))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.Cast,
            title = stringResource(R.string.cast_to_tv),
            shape = flowRowGroupShape(0, OVERLAY_ROWS),
            onClick = onCastClick,
        )
        RashNavRow(
            leadingIcon = Icons.Filled.PictureInPicture,
            title = stringResource(R.string.pip_mode),
            shape = flowRowGroupShape(1, OVERLAY_ROWS),
            onClick = onPipClick,
        )
        RashNavRow(
            leadingIcon = Icons.Filled.Bedtime,
            title = stringResource(R.string.sleep_timer),
            shape = flowRowGroupShape(2, OVERLAY_ROWS),
            onClick = onSleepTimerClick,
        )
    }

    RashSectionHeader(stringResource(R.string.audio_effects))
    RashRowGroup {
        RashNavRow(
            leadingIcon = Icons.Filled.Equalizer,
            title = stringResource(R.string.equalizer),
            trailingText = equalizerSummary(),
            shape = flowRowGroupShape(0, EFFECT_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Equalizer) },
        )
        RashSwitchRow(
            leadingIcon = Icons.Rounded.GraphicEq,
            title = stringResource(R.string.player_settings_skip_silence),
            checked = playerState.isSkipSilenceEnabled,
            shape = flowRowGroupShape(1, EFFECT_ROWS),
            onCheckedChange = onSkipSilenceToggle,
        )
        RashSwitchRow(
            leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.player_settings_normalize_volume),
            checked = playerState.isVolumeNormalizationEnabled,
            shape = flowRowGroupShape(2, EFFECT_ROWS),
            onCheckedChange = onVolumeNormalizationToggle,
        )
    }

    val glowRows = if (ambientGlowEnabled != null) 1 else 0
    val sphericalRows = listOfNotNull(sphericalViewEnabled, sphericalMotionEnabled, sphericalDragSpeed).size
    val displayRows = DISPLAY_ROWS + glowRows + sphericalRows
    RashSectionHeader(stringResource(R.string.player_settings_display))
    RashRowGroup {
        RashSwitchRow(
            leadingIcon = ImageVector.vectorResource(R.drawable.ic_ambient_mode),
            title = stringResource(R.string.player_settings_ambient_mode),
            supportingText = stringResource(R.string.player_settings_ambient_mode_subtitle).takeIf { ambientGlowEnabled != null },
            checked = ambientModeEnabled,
            shape = flowRowGroupShape(0, displayRows),
            onCheckedChange = onAmbientModeToggle,
        )
        if (ambientGlowEnabled != null) {
            RashSwitchRow(
                leadingIcon = Icons.Filled.WbTwilight,
                title = stringResource(R.string.player_settings_ambient_glow),
                supportingText = stringResource(R.string.player_settings_ambient_glow_subtitle),
                checked = ambientGlowEnabled,
                shape = flowRowGroupShape(1, displayRows),
                onCheckedChange = onAmbientGlowToggle,
            )
        }
        if (sphericalViewEnabled != null) {
            RashSwitchRow(
                leadingIcon = Icons.AutoMirrored.Filled._360,
                title = stringResource(R.string.player_settings_spherical_view),
                supportingText = stringResource(R.string.player_settings_spherical_view_summary),
                checked = sphericalViewEnabled,
                shape = flowRowGroupShape(1 + glowRows, displayRows),
                onCheckedChange = onSphericalViewToggle,
            )
        }
        if (sphericalMotionEnabled != null) {
            RashSwitchRow(
                leadingIcon = Icons.Filled.ScreenRotation,
                title = stringResource(R.string.player_settings_spherical_motion),
                supportingText = stringResource(R.string.player_settings_spherical_motion_summary),
                checked = sphericalMotionEnabled,
                shape = flowRowGroupShape(glowRows + 2, displayRows),
                onCheckedChange = onSphericalMotionToggle,
            )
        }
        if (sphericalDragSpeed != null) {
            RashNavRow(
                leadingIcon = Icons.Filled.Speed,
                title = stringResource(R.string.player_settings_spherical_drag_speed),
                trailingText = playbackSpeedLabel(sphericalDragSpeed),
                shape = flowRowGroupShape(glowRows + sphericalRows, displayRows),
                onClick = { onNavigateToPage(PlayerSettingsPage.SphericalDragSpeed) },
            )
        }
        RashNavRow(
            leadingIcon = Icons.Filled.QueryStats,
            title = stringResource(R.string.stats_for_nerds),
            shape = flowRowGroupShape(displayRows - 1, displayRows),
            onClick = onStatsForNerds,
        )
    }
}
