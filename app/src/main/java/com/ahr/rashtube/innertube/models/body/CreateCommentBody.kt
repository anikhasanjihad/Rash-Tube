package com.ahr.rashtube.innertube.models.body

import com.ahr.rashtube.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentBody(
    val context: Context,
    val createCommentParams: String,
    val commentText: String,
)
