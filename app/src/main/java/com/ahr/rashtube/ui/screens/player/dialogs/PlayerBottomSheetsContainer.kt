package com.ahr.rashtube.ui.screens.player.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahr.rashtube.R
import com.ahr.rashtube.data.model.Comment
import com.ahr.rashtube.data.model.Video
import com.ahr.rashtube.player.EnhancedPlayerManager
import com.ahr.rashtube.player.EnhancedPlayerState
import com.ahr.rashtube.player.SleepTimerManager
import com.ahr.rashtube.ui.components.shared.CommentSortFilter
import com.ahr.rashtube.ui.components.shared.RashAlertDialog
import com.ahr.rashtube.ui.components.shared.RashCommentsBottomSheet
import com.ahr.rashtube.ui.components.videoplayer.sheet.RashLiveChatBottomSheet
import com.ahr.rashtube.ui.screens.player.VideoPlayerViewModel
import com.ahr.rashtube.ui.screens.player.state.PlayerCommentsUiState
import com.ahr.rashtube.ui.screens.player.state.PlayerScreenState
import com.ahr.rashtube.ui.screens.player.state.PlayerSheet
import com.ahr.rashtube.ui.screens.player.state.VideoPlayerPreferencesState
import com.ahr.rashtube.ui.screens.player.state.VideoPlayerUiState
import com.ahr.rashtube.ui.screens.player.state.hasTranscriptTrack
import com.ahr.rashtube.ui.screens.player.state.rememberPlayerCommentsTimedFollow
import com.ahr.rashtube.ui.screens.player.state.visibleComments

@Composable
internal fun PlayerBottomSheetsContainer(
    screenState: PlayerScreenState,
    uiState: VideoPlayerUiState,
    video: Video,
    completeVideo: Video,
    disableShortsPlayer: Boolean,
    showShortsPlayerPrompt: Boolean,
    viewModel: VideoPlayerViewModel,
    playerState: EnhancedPlayerState,
    prefs: VideoPlayerPreferencesState,
    commentsUiState: PlayerCommentsUiState,
    commentsEnabled: Boolean = true,
    onLoadMoreComments: (videoId: String) -> Unit = {},
    onSelectCommentSort: (CommentSortFilter) -> Unit = {},
    mediaSheetExpandedHeight: Dp? = null,
    mediaSheetCollapsedHeight: Dp = 0.dp,
    onPlayAsShort: (String) -> Unit,
    onLoadReplies: (Comment) -> Unit = {},
    onLoadMoreReplies: (Comment) -> Unit = {},
    onNavigateToChannel: ((String) -> Unit)? = null,
    hostedInSidePanel: Boolean = false,
    onMediaSheetProgressChange: (Float) -> Unit = {},
) {
    val visibleComments = commentsUiState.visibleComments(screenState)
    val commentContext = androidx.compose.ui.platform.LocalContext.current
    val commentAccount by com.ahr.rashtube.account.YouTubeAccountStore
        .getInstance(commentContext)
        .state
        .collectAsStateWithLifecycle()

    val handleSeek: (Long) -> Unit =
        remember {
            { positionMs -> EnhancedPlayerManager.getInstance().seekTo(positionMs) }
        }

    LaunchedEffect(Unit) {
        SleepTimerManager.attachToPlayer(
            player = EnhancedPlayerManager.getInstance().getPlayer(),
        ) {
            EnhancedPlayerManager.getInstance().pause()
        }
    }

    // Comments Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Comments() && commentsEnabled && !hostedInSidePanel) {
        RashCommentsBottomSheet(
            comments = visibleComments,
            isLoading = commentsUiState.isLoading,
            selectedFilter = screenState.commentSortFilter,
            totalText = commentsUiState.totalText,
            artworkUrl = video.thumbnailUrl,
            showTimed = true,
            timedFollow = rememberPlayerCommentsTimedFollow(viewModel, screenState),
            onFilterChanged = onSelectCommentSort,
            onLoadReplies = onLoadReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onSeekMs = handleSeek,
            isLoadingMore = commentsUiState.isLoadingMore,
            hasMore = commentsUiState.hasMore,
            onLoadMore = { onLoadMoreComments(video.id) },
            onAuthorClick = { authorChannelRef ->
                screenState.closeSheet()
                onNavigateToChannel?.invoke(authorChannelRef)
            },
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
            listState = screenState.commentsListState,
            threadState = screenState.commentThreads,
            isDisabled = commentsUiState.isDisabled,
            onPostComment =
                if (commentAccount.signedIn) {
                    { text ->
                        com.ahr.rashtube.data.repository.YouTubeRepository
                            .getInstance()
                            .postVideoComment(video.id, text)
                            .isSuccess
                    }
                } else {
                    null
                },
            onSignInClick = { com.ahr.rashtube.account.YouTubeAccountActivity.launch() },
        )
    }

    if (screenState.activeSheet == PlayerSheet.LiveChat() && uiState.isLiveChatAvailable) {
        RashLiveChatBottomSheet(
            messages = uiState.liveChatMessages,
            isLoading = uiState.isLiveChatLoading,
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
        )
    }

    // Description Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Description && !hostedInSidePanel) {
        PlayerDescriptionSheetHost(
            video = video,
            uiState = uiState,
            viewModel = viewModel,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            hasTranscriptTrack = hasTranscriptTrack(playerState),
            onChaptersClick = { screenState.open(PlayerSheet.Chapters) },
            onTranscriptClick = { screenState.open(PlayerSheet.Transcript) },
            onChannelClick = { channelId ->
                screenState.closeSheet()
                onNavigateToChannel?.invoke(channelId)
            },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    if (screenState.activeSheet == PlayerSheet.Transcript && !hostedInSidePanel) {
        PlayerTranscriptSheetHost(
            viewModel = viewModel,
            screenState = screenState,
            playerState = playerState,
            prefs = prefs,
            chapters = uiState.chapters,
            artworkUrl = video.thumbnailUrl,
            exportTitle = video.title,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    // Chapters Bottom Sheet
    if (screenState.activeSheet == PlayerSheet.Chapters && !hostedInSidePanel) {
        PlayerChaptersSheetHost(
            screenState = screenState,
            chapters = uiState.chapters,
            thumbnailUrl = video.thumbnailUrl,
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    if (screenState.activeSheet == PlayerSheet.Queue && !hostedInSidePanel) {
        PlayerQueueSheetHost(
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    if (screenState.activeSheet == PlayerSheet.SleepTimer && !hostedInSidePanel) {
        PlayerSleepTimerSheetHost(
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            onDismiss = { screenState.closeSheet() },
            collapsedHeight = mediaSheetCollapsedHeight,
            onSheetProgressChange = onMediaSheetProgressChange,
        )
    }

    // Shorts Suggestion Dialog
    if (screenState.showShortsPrompt && !disableShortsPlayer && showShortsPlayerPrompt) {
        ShortsSuggestionDialog(
            onPlayAsShort = {
                screenState.showShortsPrompt = false
                onPlayAsShort(completeVideo.id)
            },
            onDismiss = { screenState.showShortsPrompt = false },
        )
    }
}

/**
 * Dialog suggesting to play a short video in the Shorts player.
 */
@Composable
private fun ShortsSuggestionDialog(
    onPlayAsShort: () -> Unit,
    onDismiss: () -> Unit,
) {
    RashAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SmartDisplay, null) },
        title = {
            Text(
                text = stringResource(R.string.play_mode_suggestion_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(stringResource(R.string.play_mode_suggestion_body))
        },
        confirmButton = {
            TextButton(onClick = onPlayAsShort) {
                Text(stringResource(R.string.shorts_player))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
