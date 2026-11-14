package com.ahr.rashtube.ui.screens.settings.content

import dagger.hilt.android.lifecycle.HiltViewModel
import com.ahr.rashtube.data.local.PlayerPreferences
import com.ahr.rashtube.data.local.PlaylistSwipeAction
import com.ahr.rashtube.data.local.PlaylistSwipes
import com.ahr.rashtube.ui.components.library.LibrarySection
import com.ahr.rashtube.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LibrarySettingsViewModel
    @Inject
    constructor(
        private val preferences: PlayerPreferences,
    ) : SettingsViewModel() {
        internal val hidden = preferences.hiddenLibrarySections.map(LibrarySection::fromStored).asState(emptySet())
        val separatePlaylists = preferences.separatePlaylistKinds.asState(false)
        val shortsContent = preferences.shortsContentEnabled.asState(true)
        val notes = preferences.notesEnabled.asState(true)
        val playlistSwipes = preferences.playlistSwipeActions.asState(PlaylistSwipes())

        internal fun setShown(
            section: LibrarySection,
            shown: Boolean,
        ) = write { preferences.setLibrarySectionHidden(section.name, hidden = !shown) }

        fun setPlaylistSwipeLeft(action: PlaylistSwipeAction) = write { preferences.setPlaylistSwipeLeftAction(action) }

        fun setPlaylistSwipeRight(action: PlaylistSwipeAction) = write { preferences.setPlaylistSwipeRightAction(action) }
    }
