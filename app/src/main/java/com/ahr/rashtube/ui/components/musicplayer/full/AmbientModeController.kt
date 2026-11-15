package com.ahr.rashtube.ui.components.musicplayer.full

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether Ambient Mode is on. MainActivity watches this and turns the screen landscape itself, so the
 * rotation never depends on which context the overlay's dialog happens to be composed in.
 */
object AmbientModeController {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    fun set(on: Boolean) {
        _active.value = on
    }
}
