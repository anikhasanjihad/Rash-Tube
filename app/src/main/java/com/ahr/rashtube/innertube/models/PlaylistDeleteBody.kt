package com.ahr.rashtube.innertube.models.body

import com.ahr.rashtube.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
