package com.ahr.rashtube.ui.screens.settings.content

import dagger.hilt.android.lifecycle.HiltViewModel
import com.ahr.rashtube.data.local.PlayerPreferences
import com.ahr.rashtube.ui.components.music.section.MusicHomeShelf
import com.ahr.rashtube.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MusicHomeSettingsViewModel
    @Inject
    constructor(
        private val preferences: PlayerPreferences,
    ) : SettingsViewModel() {
        val hidden = preferences.hiddenMusicHomeShelves.map(MusicHomeShelf::fromStored).asState(emptySet())

        fun setShown(
            shelf: MusicHomeShelf,
            shown: Boolean,
        ) = write { preferences.setMusicHomeShelfHidden(shelf.name, hidden = !shown) }
    }
