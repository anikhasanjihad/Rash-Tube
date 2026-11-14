package com.ahr.rashtube.ui.screens.settings.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ahr.rashtube.R
import com.ahr.rashtube.ui.components.settings.SettingsPage
import com.ahr.rashtube.ui.components.settings.nav
import com.ahr.rashtube.ui.components.settings.notice
import com.ahr.rashtube.ui.screens.settings.index.WidgetsIndex
import com.ahr.rashtube.widget.core.RashWidgets
import kotlinx.coroutines.launch

/** Every Flow widget, each one tap from the launcher's own add-widget dialog. */
@Composable
internal fun WidgetsSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val canPin = remember(context) { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    val unsupported = stringResource(R.string.settings_widgets_unsupported)

    SettingsPage(
        title = stringResource(R.string.settings_widgets_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
    ) {
        if (!canPin) notice("widgets.unsupported", text = { unsupported })
        group(key = "widgets.add", header = R.string.settings_widgets_add_header, footer = R.string.settings_widgets_footer) {
            RashWidgets.catalog.forEach { widget ->
                nav(
                    WidgetsIndex.entryFor(widget),
                    onClick = {
                        scope.launch {
                            // Straight to the platform pin request: Glance's own call also composes a preview
                            // widget, and a failure there is what the launcher reports as "Couldn't load".
                            val requested =
                                runCatching {
                                    AppWidgetManager
                                        .getInstance(context)
                                        .requestPinAppWidget(ComponentName(context, widget.receiver.java), null, null)
                                }.getOrDefault(false)
                            if (!requested) snackbarHostState.showSnackbar(unsupported)
                        }
                    },
                    enabled = canPin,
                    showChevron = false,
                    iconRes = widget.icon,
                )
            }
        }
    }
}
