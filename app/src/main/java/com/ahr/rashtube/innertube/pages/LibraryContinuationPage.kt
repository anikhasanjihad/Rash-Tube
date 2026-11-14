package com.ahr.rashtube.innertube.pages

import com.ahr.rashtube.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
