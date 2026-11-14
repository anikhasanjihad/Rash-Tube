package com.ahr.rashtube.ui.components.layout.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.ahr.rashtube.ui.components.shared.RashSearchField

/**
 * Search variant of [RashTopBar]: back button, inline field, optional trailing actions.
 *
 * Used for in-place filtering of a screen's own content (Settings search, Subscriptions manage
 * mode), not for the Search tab. Global actions are never shown — the user is in a focused mode and
 * the leading affordance dismisses it rather than navigating back.
 */
@Composable
fun RashSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    autoFocus: Boolean = true,
    releaseFocusWithKeyboard: Boolean = false,
    windowInsets: WindowInsets = RashTopBarDefaults.WindowInsets,
) {
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    RashTopBar(
        modifier = modifier,
        title = {
            RashSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth(),
                focusRequester = focusRequester,
                releaseFocusWithKeyboard = releaseFocusWithKeyboard,
            )
        },
        onBack = onClose,
        actions = actions,
        globalActions = RashGlobalActionsMode.None,
        windowInsets = windowInsets,
    )
}
