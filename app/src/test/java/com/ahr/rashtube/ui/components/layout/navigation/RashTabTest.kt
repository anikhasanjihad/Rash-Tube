package com.ahr.rashtube.ui.components.layout.navigation

import com.ahr.rashtube.data.local.DEFAULT_NAV_TAB_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RashTabTest {
    @Test
    fun hiddenHomeFallsBackToFirstVisibleDestination() {
        val visibility = NavigationVisibility(home = false, shorts = false, music = false)

        val resolved =
            resolveDefaultFlowTab(
                preferredId = RashTab.Home.id,
                order = listOf(0, 4, 3, 1, 2, 5, 6),
                visibility = visibility,
            )

        assertEquals(RashTab.Library, resolved)
        assertFalse(visibleFlowTabs(listOf(0, 4, 3), visibility).contains(RashTab.Home))
    }

    @Test
    fun reEnabledHomeRestoresAHomeDefault() {
        val resolved =
            resolveDefaultFlowTab(
                preferredId = RashTab.Home.id,
                order = listOf(3, 0, 4),
                visibility = NavigationVisibility(home = true),
            )

        assertEquals(RashTab.Home, resolved)
    }

    @Test
    fun tabsFollowTheSavedOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(4, 3, 2, 1, 0),
                visibility = NavigationVisibility(),
            )

        assertEquals(
            listOf(RashTab.Library, RashTab.Subscriptions, RashTab.Music, RashTab.Shorts, RashTab.Home),
            tabs,
        )
    }

    @Test
    fun aSavedOrderMissingNewerTabsAppendsThemInDefaultOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(3, 0),
                visibility = NavigationVisibility(search = true, categories = true),
            )

        assertEquals(
            listOf(
                RashTab.Subscriptions,
                RashTab.Home,
                RashTab.Shorts,
                RashTab.Music,
                RashTab.Library,
                RashTab.Search,
                RashTab.Explore,
            ),
            tabs,
        )
    }

    @Test
    fun unknownPersistedIdsAreIgnored() {
        val tabs = visibleFlowTabs(order = listOf(42, 4, -1, 3), visibility = NavigationVisibility())

        assertEquals(RashTab.Library, tabs.first())
        assertEquals(tabs.distinct(), tabs)
    }

    @Test
    fun persistedIdsNeverChange() {
        assertEquals(DEFAULT_NAV_TAB_ORDER, RashTab.entries.map(RashTab::id))
        RashTab.entries.forEach { tab -> assertEquals(tab, RashTab.fromId(tab.id)) }
    }

    /**
     * Settings and Notifications live in the top bar of every root destination, which only works if
     * a root destination always exists. Subscriptions and Library are unconditional in
     * [visibleFlowTabs]; this pins that down so hiding tabs can never orphan those screens.
     */
    @Test
    fun everyVisibilityCombinationKeepsAnUnhideableRootDestination() {
        val orders =
            listOf(
                DEFAULT_NAV_TAB_ORDER,
                listOf(6, 5, 4, 3, 2, 1, 0),
                listOf(4, 3),
                emptyList(),
            )

        for (bits in 0 until 32) {
            val visibility =
                NavigationVisibility(
                    home = bits and 1 != 0,
                    shorts = bits and 2 != 0,
                    music = bits and 4 != 0,
                    search = bits and 8 != 0,
                    categories = bits and 16 != 0,
                )

            for (order in orders) {
                val visible = visibleFlowTabs(order, visibility)
                assertTrue(
                    "no unhideable root destination for $visibility / $order",
                    visible.contains(RashTab.Subscriptions) || visible.contains(RashTab.Library),
                )

                val resolved = resolveDefaultFlowTab(RashTab.Home.id, order, visibility)
                assertTrue(
                    "resolved default $resolved is not visible for $visibility / $order",
                    visible.contains(resolved),
                )
            }
        }
    }

    @Test
    fun theDownloadsTabIsOffUntilTurnedOn() {
        assertFalse(RashTab.Downloads in visibleFlowTabs(DEFAULT_NAV_TAB_ORDER, NavigationVisibility()))
        assertEquals(
            RashTab.Downloads,
            visibleFlowTabs(listOf(3, 0), NavigationVisibility(downloads = true)).last(),
        )
    }
}
