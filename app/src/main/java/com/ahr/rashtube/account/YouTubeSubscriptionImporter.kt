/*
 * Rash Tube — imports the signed-in account's subscriptions.
 * Part of Rash Tube by AHR, a modified version of Flow (A-EDev), licensed under GPL-3.0.
 */
package com.ahr.rashtube.account

import android.content.Context
import android.util.Log
import com.ahr.rashtube.data.local.ChannelSubscription
import com.ahr.rashtube.data.local.SubscriptionRepository
import com.ahr.rashtube.innertube.YouTube
import com.ahr.rashtube.innertube.models.ArtistItem
import com.ahr.rashtube.innertube.models.YTItem

object YouTubeSubscriptionImporter {
    private const val TAG = "YouTubeSubscriptionImporter"
    private const val MUSIC_BROWSE_ID = "FEmusic_library_corpus_artists"
    private const val MAX_PAGES = 40

    /**
     * Copies the account's subscribed channels into the local subscription list; returns how many
     * were found. The channels a viewer watches (youtube.com) and the artists in the music library
     * are two separate lists, so each is read on its own: one of them failing, or being empty,
     * no longer loses the other.
     */
    suspend fun import(context: Context): Result<Int> {
        val found = LinkedHashMap<String, ChannelSubscription>()

        // Content channels go in first, so a channel that is in both lists stays a regular channel.
        val content = runCatching { collectContentChannels(found) }
        content.exceptionOrNull()?.let { Log.w(TAG, "Content subscriptions failed: ${it::class.simpleName}: ${it.message}") }

        val music = runCatching { collectMusicArtists(found) }
        music.exceptionOrNull()?.let { Log.w(TAG, "Music subscriptions failed: ${it::class.simpleName}: ${it.message}") }

        if (content.isFailure && music.isFailure) {
            return Result.failure(content.exceptionOrNull() ?: music.exceptionOrNull() ?: IllegalStateException("Import failed"))
        }
        return runCatching {
            SubscriptionRepository.getInstance(context).subscribeAll(found.values)
            found.size
        }
    }

    private suspend fun collectContentChannels(found: MutableMap<String, ChannelSubscription>) {
        YouTube.accountSubscribedChannels(MAX_PAGES).getOrThrow().forEach { channel ->
            if (channel.id.isBlank()) return@forEach
            found.putIfAbsent(
                channel.id,
                ChannelSubscription(
                    channelId = channel.id,
                    channelName = channel.name,
                    channelThumbnail = channel.thumbnailUrl.let { if (it.startsWith("//")) "https:$it" else it },
                    isMusic = false,
                ),
            )
        }
    }

    private suspend fun collectMusicArtists(found: MutableMap<String, ChannelSubscription>) {
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

        val first = YouTube.library(MUSIC_BROWSE_ID).getOrThrow()
        collect(first.items)
        var token = first.continuation
        var pages = 0
        while (token != null && pages < MAX_PAGES) {
            val next = YouTube.libraryContinuation(token).getOrThrow()
            collect(next.items)
            token = next.continuation
            pages++
        }
    }
}
