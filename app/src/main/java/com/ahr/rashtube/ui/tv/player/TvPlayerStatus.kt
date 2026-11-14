package com.ahr.rashtube.ui.tv.player

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahr.rashtube.ui.components.shared.RashErrorState
import com.ahr.rashtube.ui.components.shared.RashLoadingIndicator

private val ErrorMaxWidth = 560.dp

/** The spinner while a video loads with the controls hidden, or the load error with a retry. */
@Composable
internal fun TvPlayerStatus(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loading) RashLoadingIndicator(modifier = modifier)
    if (error != null) {
        Surface(
            modifier = modifier.widthIn(max = ErrorMaxWidth),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            RashErrorState(error = error, onRetry = onRetry)
        }
    }
}
