/*
 * Rash Tube — imports the signed-in account's subscriptions.
 * Part of Rash Tube by AHR, a modified version of Flow (A-EDev), licensed under GPL-3.0.
 */
package com.ahr.rashtube.account

import android.content.Context
import com.ahr.rashtube.data.local.ChannelSubscription
import com.ahr.rashtube.data.local.SubscriptionRepository
import com.ahr.rashtube.innertube.YouTube
import com.ahr.rashtube.innertube.models.ArtistItem
import com.ahr.rashtube.innertube.models.YTItem

object YouTubeSubscriptionImporter {
    private const val SUBSCRIPTIONS_BROWSE_ID = "FEmusic_library_corpus_artists"
    private const val MAX_PAGES = 40

    /** Copies the account's subscribed channels into the local subscription list; returns how many were found. */
    suspend fun import(context: Context): Result<Int> =
        runCatching {
            val found = LinkedHashMap<String, ChannelSubscription>()

            fun collect(items: List<YTItem>) {
                items.filterIsInstance<ArtistItem>().forEach { artist ->
                    val id = artist.channelId?.takeIf { it.isNotBlank() } ?: artist.id
                    if (id.isNotBlank()) {
                        found.putIfAbsent(
                            id,
                            ChannelSubscription(
                                channelId = id,
                                channelName = artist.title,
                                channelThumbnail = artist.thumbnail.orEmpty(),
                                isMusic = !artist.isProfile,
                            ),
                        )
                    }
                }
            }

            val first = YouTube.library(SUBSCRIPTIONS_BROWSE_ID).getOrThrow()
            collect(first.items)
            var token = first.continuation
            var pages = 0
            while (token != null && pages < MAX_PAGES) {
                val next = YouTube.libraryContinuation(token).getOrThrow()
                collect(next.items)
                token = next.continuation
                pages++
            }

            SubscriptionRepository.getInstance(context).subscribeAll(found.values)
            found.size
        }
}
