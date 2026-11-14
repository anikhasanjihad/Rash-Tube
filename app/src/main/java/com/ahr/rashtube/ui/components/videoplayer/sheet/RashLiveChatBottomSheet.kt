package com.ahr.rashtube.ui.components.videoplayer.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahr.rashtube.R
import com.ahr.rashtube.data.model.LiveChatMessage
import com.ahr.rashtube.ui.components.shared.RashBottomSheet
import com.ahr.rashtube.ui.components.shared.RashSheetHeader
import com.ahr.rashtube.ui.components.shared.defaultSheetExpandedHeight
import com.ahr.rashtube.ui.components.shared.rememberFlowBottomSheetState

// Draggable live-chat bottom sheet (portrait)
@Composable
fun RashLiveChatBottomSheet(
    messages: List<LiveChatMessage>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberFlowBottomSheetState()
    RashBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissOnOutsideTap = false,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            RashSheetHeader(
                title = stringResource(R.string.live_chat),
                onClose = { sheetState.dismiss() },
                modifier = dragModifier,
            )
        },
    ) {
        LiveChatList(
            messages = messages,
            isLoading = isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
        )
    }
}
