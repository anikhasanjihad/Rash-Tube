package com.ahr.rashtube.innertube.pages

import com.ahr.rashtube.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
