package com.ahr.rashtube.innertube.pages

import com.ahr.rashtube.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
