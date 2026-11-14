package com.ahr.rashtube.ui.screens.playlists

import com.ahr.rashtube.data.model.PlaylistInfo
import com.ahr.rashtube.data.playlist.PlaylistListOrder
import com.ahr.rashtube.data.playlist.sortedFor
import com.ahr.rashtube.ui.components.library.PlaylistOwnershipFilter

/**
 * Owned playlists lead saved ones in every order except [PlaylistListOrder.CUSTOM], where the
 * viewer's own arrangement decides.
 */
internal fun orderedPlaylists(
    owned: List<PlaylistInfo>,
    saved: List<PlaylistInfo>,
    filter: PlaylistOwnershipFilter,
    order: PlaylistListOrder,
): List<PlaylistInfo> =
    if (order == PlaylistListOrder.CUSTOM) {
        filter.select(owned, saved).sortedFor(order)
    } else {
        filter.select(owned.sortedFor(order), saved.sortedFor(order))
    }
