package com.ahr.rashtube.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ahr.rashtube.R
import com.ahr.rashtube.ui.components.shared.RashEmptyState

@Composable
internal fun HomeFeedDisabledState(modifier: Modifier = Modifier) {
    RashEmptyState(
        title = stringResource(R.string.content_settings_home_feed_disabled_title),
        subtitle = stringResource(R.string.content_settings_home_feed_disabled_body),
        icon = Icons.Outlined.SmartDisplay,
        modifier = modifier,
    )
}
