package com.ahr.rashtube.ui.screens.music

import com.ahr.rashtube.data.local.PlaylistRepository
import com.ahr.rashtube.data.music.model.PlaylistDetails
import com.ahr.rashtube.ui.screens.music.collection.toStoredVideo

/** Saves an album or playlist to the library with its tracks, so it plays offline from the library. */
internal suspend fun PlaylistRepository.saveMusicCollection(details: PlaylistDetails) {
    saveExternalMusicPlaylist(
        id = details.id,
        name = details.title,
        description = details.description.orEmpty(),
        thumbnailUrl = details.thumbnailUrl,
    )
    addVideosToPlaylist(details.id, details.tracks.map { it.toStoredVideo() })
}
