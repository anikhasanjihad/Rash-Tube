package com.ahr.rashtube.ui.components.shared

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahr.rashtube.ui.theme.frostColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.layout.navigationBarsPadding
import com.ahr.rashtube.R
import com.ahr.rashtube.data.model.Comment
import com.ahr.rashtube.ui.components.shared.RashBottomSheet
import com.ahr.rashtube.ui.components.shared.defaultSheetExpandedHeight
import com.ahr.rashtube.ui.components.shared.rememberFlowBottomSheetState

@Composable
fun RashCommentsBottomSheet(
    comments: List<Comment>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSeekMs: (Long) -> Unit = {},
    onFilterChanged: (CommentSortFilter) -> Unit = {},
    onLoadReplies: (Comment) -> Unit = {},
    onLoadMoreReplies: (Comment) -> Unit = {},
    selectedFilter: CommentSortFilter = CommentSortFilter.TOP,
    totalText: String? = null,
    artworkUrl: String? = null,
    showTimed: Boolean = false,
    timedFollow: CommentsTimedFollow? = null,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    onAuthorClick: (String) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
    dismissOnOutsideTap: Boolean = false,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    threadState: CommentThreadState? = null,
    isDisabled: Boolean = false,
    /** Set when the signed-in account can comment: posts the text and reports whether it worked. */
    onPostComment: (suspend (String) -> Boolean)? = null,
    /** Shown in place of the box while nobody is signed in: opens the YouTube sign-in. */
    onSignInClick: (() -> Unit)? = null,
) {
    val sheetState = rememberFlowBottomSheetState()
    val tint = rememberMediaArtworkTint(artworkUrl)

    RashBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissOnOutsideTap = dismissOnOutsideTap,
        shape = RectangleShape,
        // Frosted: dense enough that the page moving behind never shows through the list or the box.
        containerColor = frostColor().copy(alpha = 0.96f),
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            // Not RashSheetHeader: the sort chips sit under the title row, inside the same padding
            // and above the divider, which no header parameter can express.
            CommentsSheetHeader(
                selectedFilter = selectedFilter,
                totalText = totalText,
                showTimed = showTimed,
                timedFollow = timedFollow,
                onFilterChanged = onFilterChanged,
                onClose = { sheetState.dismiss() },
                dragModifier = dragModifier,
            )
        },
    ) {
        RashCommentsList(
            comments = comments,
            isLoading = isLoading,
            listState = listState,
            selectedFilter = selectedFilter,
            onSeekMs = onSeekMs,
            onLoadReplies = onLoadReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onAuthorClick = onAuthorClick,
            onAvatarClick = onAvatarClick,
            isLoadingMore = isLoadingMore,
            onLoadMore = onLoadMore,
            hasMore = hasMore,
            tint = tint,
            timedFollow = timedFollow,
            threadState = threadState,
            isDisabled = isDisabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        )
        if (!isDisabled) {
            if (onPostComment != null) {
                CommentComposer(onPostComment)
            } else if (onSignInClick != null) {
                SignInToComment(onSignInClick)
            }
        }
    }
}

/** The poster for the signed-in YouTube account, or null while nobody is signed in. [videoId] is read at post time. */
@Composable
fun rememberCommentPoster(videoId: () -> String?): (suspend (String) -> Boolean)? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val account by com.ahr.rashtube.account.YouTubeAccountStore
        .getInstance(context)
        .state
        .collectAsStateWithLifecycle()
    val latestId by androidx.compose.runtime.rememberUpdatedState(videoId)
    val poster =
        androidx.compose.runtime.remember<suspend (String) -> Boolean> {
            { text ->
                val id = latestId()
                id != null &&
                    com.ahr.rashtube.data.repository.YouTubeRepository
                        .getInstance()
                        .postVideoComment(id, text)
                        .isSuccess
            }
        }
    return if (account.signedIn) poster else null
}

@Composable
private fun SignInToComment(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.comment_sign_in),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun CommentComposer(onPost: suspend (String) -> Boolean) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var posting by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var failed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val errorText: (@Composable () -> Unit)? =
        if (failed) {
            { androidx.compose.material3.Text(stringResource(R.string.comment_post_failed)) }
        } else {
            null
        }
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                failed = false
            },
            modifier = Modifier.weight(1f),
            placeholder = { androidx.compose.material3.Text(stringResource(R.string.add_comment_placeholder)) },
            maxLines = 4,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            isError = failed,
            enabled = !posting,
            supportingText = errorText,
            colors =
                androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                ),
        )
        androidx.compose.material3.IconButton(
            enabled = text.isNotBlank() && !posting,
            onClick = {
                scope.launch {
                    posting = true
                    val ok = onPost(text.trim())
                    posting = false
                    if (ok) text = "" else failed = true
                }
            },
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.comment_post),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheetHeader(
    selectedFilter: CommentSortFilter,
    totalText: String?,
    showTimed: Boolean,
    timedFollow: CommentsTimedFollow?,
    onFilterChanged: (CommentSortFilter) -> Unit,
    onClose: () -> Unit,
    dragModifier: Modifier,
) {
    Column(modifier = dragModifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BottomSheetDefaults.DragHandle()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.comments),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!totalText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
            CommentSortChips(
                selected = selectedFilter,
                onSelect = onFilterChanged,
                showTimed = showTimed,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selectedFilter == CommentSortFilter.TIMED && timedFollow != null) {
            CommentsTimedFollowRow(timedFollow)
        }
        HorizontalDivider(
            color =
                MaterialTheme.colorScheme.outlineVariant
                    .copy(alpha = RashSheetHeaderDefaults.DividerAlpha),
        )
    }
}
