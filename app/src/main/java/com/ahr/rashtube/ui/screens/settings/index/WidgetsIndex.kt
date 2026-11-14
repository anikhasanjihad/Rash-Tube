package com.ahr.rashtube.ui.screens.settings.index

import com.ahr.rashtube.R
import com.ahr.rashtube.ui.components.settings.SettingEntry
import com.ahr.rashtube.ui.components.settings.SettingsDestination
import com.ahr.rashtube.widget.core.RashWidgetEntry
import com.ahr.rashtube.widget.core.RashWidgets

internal object WidgetsIndex {
    fun entryFor(widget: RashWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = RashWidgets.catalog.map(::entryFor)
}
