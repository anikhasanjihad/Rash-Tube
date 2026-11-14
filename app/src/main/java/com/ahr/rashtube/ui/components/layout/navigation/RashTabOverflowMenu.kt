package com.ahr.rashtube.ui.components.layout.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Splits [tabs] into the ones a navigation surface with room for [capacity] items shows and the
 * ones behind its "More" item, which takes the last slot.
 */
internal fun splitTabsForOverflow(
    tabs: List<RashTab>,
    capacity: Int,
): Pair<List<RashTab>, List<RashTab>> {
    if (tabs.size <= capacity) return tabs to emptyList()
    val shown = tabs.take((capacity - 1).coerceAtLeast(0))
    return shown to tabs.drop(shown.size)
}

/** The menu a navigation surface's "More" item opens, listing the tabs it had no room for. */
@Composable
internal fun RashTabOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    tabs: List<RashTab>,
    selectedTab: RashTab?,
    onTabSelected: (RashTab) -> Unit,
) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
            tabs.forEachIndexed { index, tab ->
                DropdownMenuItem(
                    selected = tab == selectedTab,
                    onClick = {
                        onDismissRequest()
                        onTabSelected(tab)
                    },
                    text = { Text(text = stringResource(tab.labelRes)) },
                    shapes = MenuDefaults.itemShape(index = index, count = tabs.size),
                    leadingIcon = {
                        Icon(
                            imageVector = tab.icon(selected = false),
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                        )
                    },
                    selectedLeadingIcon = {
                        Icon(
                            imageVector = tab.icon(selected = true),
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                        )
                    },
                )
            }
        }
    }
}
