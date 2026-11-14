package com.ahr.rashtube.innertube.models.body

import com.ahr.rashtube.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlBody(
    val context: Context,
    val url: String,
)
