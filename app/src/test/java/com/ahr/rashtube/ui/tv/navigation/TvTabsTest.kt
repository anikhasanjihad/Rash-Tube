package com.ahr.rashtube.ui.tv.navigation

import com.google.common.truth.Truth.assertThat
import com.ahr.rashtube.ui.components.layout.navigation.RashTab
import com.ahr.rashtube.ui.components.layout.navigation.NavigationVisibility
import org.junit.Test

class TvTabsTest {
    private val allOn = NavigationVisibility(home = true, shorts = true, music = true, search = true, categories = true)

    @Test
    fun `follows the user's order and leaves Shorts and Explore off`() {
        val order = listOf(RashTab.Library, RashTab.Shorts, RashTab.Music, RashTab.Explore, RashTab.Home).map(RashTab::id)

        val tabs = tvNavigation(order, defaultTabId = RashTab.Home.id, visibility = allOn).tabs

        assertThat(tabs.take(3)).containsExactly(RashTab.Library, RashTab.Music, RashTab.Home).inOrder()
        assertThat(tabs).containsNoneOf(RashTab.Shorts, RashTab.Explore)
    }

    @Test
    fun `always shows Search, which TV has nowhere else to put`() {
        val tabs = tvNavigation(emptyList(), RashTab.Home.id, NavigationVisibility(search = false)).tabs

        assertThat(tabs).contains(RashTab.Search)
    }

    @Test
    fun `hides the tabs the user hid`() {
        val tabs = tvNavigation(emptyList(), RashTab.Home.id, NavigationVisibility(home = false, music = false)).tabs

        assertThat(tabs).containsNoneOf(RashTab.Home, RashTab.Music)
    }

    @Test
    fun `opens on the user's default tab, or the first tab when TV leaves it off`() {
        assertThat(tvNavigation(emptyList(), RashTab.Library.id, allOn).startTab).isEqualTo(RashTab.Library)

        val fromShorts = tvNavigation(emptyList(), RashTab.Shorts.id, allOn)
        assertThat(fromShorts.startTab).isEqualTo(fromShorts.tabs.first())
    }

    @Test
    fun `roots are the tabs plus Settings`() {
        val navigation = tvNavigation(emptyList(), RashTab.Home.id, allOn)

        assertThat(navigation.rootRoutes).containsExactlyElementsIn(navigation.tabs.map(RashTab::route) + TV_SETTINGS_ROUTE)
    }
}
