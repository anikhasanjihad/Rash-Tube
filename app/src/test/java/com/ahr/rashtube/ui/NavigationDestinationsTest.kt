package com.ahr.rashtube.ui

import com.ahr.rashtube.ui.components.layout.navigation.RashTab
import com.ahr.rashtube.ui.components.layout.navigation.NavigationVisibility
import com.ahr.rashtube.ui.components.layout.navigation.visibleFlowTabs
import com.ahr.rashtube.ui.screens.update.UPDATE_ROUTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun tabRootsMapToTheirTab() {
        RashTab.entries.filter { it != RashTab.Shorts }.forEach { tab ->
            assertEquals(tab, flowTabForDestination(tab.route, shortsSourceArg = null))
        }
    }

    @Test
    fun onlyTheShortsFeedIsTheShortsTab() {
        assertEquals(RashTab.Shorts, flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = null))
        assertEquals(RashTab.Shorts, flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed"))
        assertNull(flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed:dQw4w9WgXcQ"))
        assertNull(flowTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "saved:"))
    }

    @Test
    fun aVideoOpenedFromShortsPlaysOverTheFirstOtherTab() {
        assertEquals(RashTab.Music, exitTab(listOf(RashTab.Shorts, RashTab.Music, RashTab.Library), RashTab.Shorts))
        assertEquals(RashTab.Home, exitTab(listOf(RashTab.Home, RashTab.Shorts), RashTab.Shorts))
        assertEquals(RashTab.Home, exitTab(listOf(RashTab.Shorts), RashTab.Shorts))
        assertEquals(RashTab.Home, exitTab(emptyList(), RashTab.Shorts))
    }

    @Test
    fun backFromSearchAsTheOpeningTabLandsOnAVisibleTab() {
        val homeHidden =
            visibleFlowTabs(listOf(5, 0, 3, 4), NavigationVisibility(home = false, shorts = false, music = false, search = true))
        assertEquals(RashTab.Search, homeHidden.first())
        assertEquals(RashTab.Subscriptions, exitTab(homeHidden, RashTab.Search))
    }

    @Test
    fun detailScreensAreNotTabRoots() {
        listOf("settings", "settings/content", "playlists", "playlist/{playlistId}", "onboarding", null).forEach { route ->
            assertNull(flowTabForDestination(route, shortsSourceArg = null))
        }
    }

    @Test
    fun routesFromOutsideTheGraphResolveToTheirTab() {
        assertEquals(RashTab.Search, flowTabForRoute("search", downloadsTab = false))
        assertEquals(RashTab.Music, flowTabForRoute("music", downloadsTab = false))
        listOf("downloads", "history", "musicRecognize", "musicPlayer/abc", "settings").forEach { route ->
            assertNull(flowTabForRoute(route, downloadsTab = false))
        }
    }

    @Test
    fun theDownloadsPageOpensAsTheTabOnlyWhenTheBarHasIt() {
        assertEquals(RashTab.Downloads, flowTabForRoute("downloads", downloadsTab = true))
        assertNull(flowTabForRoute("downloads", downloadsTab = false))
        assertNull(flowTabForDestination("downloads", shortsSourceArg = null))
    }

    @Test
    fun searchIsTheOnlyTabWithoutTheBar() {
        RashTab.entries.forEach { tab -> assertEquals(tab != RashTab.Search, tab.showsNavigationBar()) }
        assertFalse((null as RashTab?).showsNavigationBar())
        assertTrue(RashTab.Home.showsNavigationBar())
    }

    @Test
    fun theSwitchPutsTheBarOnSearchOnly() {
        RashTab.entries.forEach { tab -> assertTrue(tab.showsNavigationBar(onSearch = true)) }
        assertFalse((null as RashTab?).showsNavigationBar(onSearch = true))
    }

    @Test
    fun onlyChannelIdsAreBrowsedDirectly() {
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("UCXuqSBlHAE6Xw-yeJA0Tunw"))
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("https://m.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw?si=abc"))
        assertNull(youtubeChannelBrowseId("@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/c/LinusTechTips"))
        assertNull(youtubeChannelBrowseId(" "))
    }

    @Test
    fun channelHandlesUseHandleUrls() {
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("@flow"))
        assertEquals("https://www.youtube.com/@flow", youtubeChannelUrl("flow"))
        assertEquals(
            "https://www.youtube.com/channel/UC123",
            youtubeChannelUrl("UC123"),
        )
    }

    @Test
    fun malformedHandleChannelUrlsAreRepaired() {
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://youtube.com/channel/@flow"),
        )
        assertEquals(
            "https://www.youtube.com/@flow",
            youtubeChannelUrl("https://m.youtube.com/@flow/videos"),
        )
    }

    @Test
    fun channelRoutesEncodeCanonicalUrls() {
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2F%40flow",
            youtubeChannelRoute("@flow"),
        )
    }

    @Test
    fun aTabTappedOnSearchOpenedAsAPageLeavesSearchFirst() {
        RashTab.entries.filter { it != RashTab.Search }.forEach { tapped ->
            assertEquals(
                TabTap.LEAVE_SEARCH,
                tabTapAction(tapped, RashTab.Search, selectedTab = RashTab.Home, searchIsTab = false, startTab = RashTab.Home),
            )
        }
    }

    @Test
    fun withASearchTabInTheBarOtherTabsSwitchAndKeepItsResults() {
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(RashTab.Music, RashTab.Search, selectedTab = RashTab.Search, searchIsTab = true, startTab = RashTab.Home),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(RashTab.Search, RashTab.Search, selectedTab = RashTab.Search, searchIsTab = true, startTab = RashTab.Home),
        )
    }

    @Test
    fun theStartTabLeavesTheSearchTabLikeBack() {
        assertEquals(
            TabTap.LEAVE_SEARCH,
            tabTapAction(RashTab.Home, RashTab.Search, selectedTab = RashTab.Search, searchIsTab = true, startTab = RashTab.Home),
        )
        assertEquals(
            TabTap.LEAVE_SEARCH,
            tabTapAction(RashTab.Music, RashTab.Search, selectedTab = RashTab.Search, searchIsTab = true, startTab = RashTab.Music),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(RashTab.Search, RashTab.Search, selectedTab = RashTab.Search, searchIsTab = true, startTab = RashTab.Search),
        )
    }

    @Test
    fun theTabAlreadyOnItsFirstScreenScrollsToTheTop() {
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(RashTab.Home, RashTab.Home, selectedTab = RashTab.Home, searchIsTab = false, startTab = RashTab.Home),
        )
        assertEquals(
            TabTap.SCROLL_TO_TOP,
            tabTapAction(RashTab.Music, RashTab.Music, selectedTab = RashTab.Music, searchIsTab = true, startTab = RashTab.Home),
        )
    }

    @Test
    fun theHighlightedTabOnAPageInsideItGoesBackToItsFirstScreen() {
        assertEquals(
            TabTap.POP_TO_ROOT,
            tabTapAction(RashTab.Home, currentTab = null, selectedTab = RashTab.Home, searchIsTab = false, startTab = RashTab.Home),
        )
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(RashTab.Music, currentTab = null, selectedTab = RashTab.Home, searchIsTab = false, startTab = RashTab.Home),
        )
    }

    @Test
    fun anotherTabSwitches() {
        assertEquals(
            TabTap.SWITCH,
            tabTapAction(RashTab.Music, RashTab.Home, selectedTab = RashTab.Home, searchIsTab = false, startTab = RashTab.Home),
        )
    }

    @Test
    fun innerPagesKeepTheBarOnlyWhenAskedFor() {
        assertTrue(navigationBarShown(null, "channel?url={channelUrl}", onSearch = false, onInnerPages = true))
        assertTrue(navigationBarShown(null, MUSIC_ARTIST_ROUTE_PATTERN, onSearch = false, onInnerPages = true))
        assertFalse(navigationBarShown(null, "channel?url={channelUrl}", onSearch = false, onInnerPages = false))
    }

    @Test
    fun settingsPlayersAndFullScreenFlowsNeverGetTheBar() {
        listOf("settings?target={target}", EQUALIZER_ROUTE, UPDATE_ROUTE, MUSIC_PLAYER_ROUTE_PATTERN, "player/{videoId}", "onboarding")
            .forEach { route -> assertFalse(route, navigationBarShown(null, route, onSearch = true, onInnerPages = true)) }
    }

    @Test
    fun tabRootsAndSearchKeepTheirOwnRules() {
        assertTrue(navigationBarShown(RashTab.Home, "home", onSearch = false, onInnerPages = false))
        assertFalse(navigationBarShown(RashTab.Search, "search", onSearch = false, onInnerPages = true))
        assertTrue(navigationBarShown(RashTab.Search, "search", onSearch = true, onInnerPages = false))
    }

    @Test
    fun everySearchPageIsTheSearchTab() {
        assertEquals(
            RashTab.Search,
            flowTabForDestination(com.ahr.rashtube.ui.screens.search.SEARCH_ROUTE_PATTERN, shortsSourceArg = null),
        )
    }
}
