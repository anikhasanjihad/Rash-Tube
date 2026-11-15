package com.ahr.rashtube.innertube.pages.renderer

import com.ahr.rashtube.innertube.pages.objectOrNull
import com.ahr.rashtube.innertube.pages.stringOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** What one signed-in browse page held: its items in page order and the token for the next page. */
internal data class AccountBrowsePage(
    val items: List<FeedItem>,
    val continuation: String?,
)

/**
 * Reads an account browse response (the home feed, the subscription list) into [FeedItem]s.
 *
 * The two pages nest their items differently (rich grid, section list, shelves) and YouTube keeps
 * reshaping them, so this walks the whole response and lets the shared renderer registry decide
 * what is an item, the same registry the search page uses. Ad slots are skipped.
 */
internal fun JsonObject.toAccountBrowsePage(): AccountBrowsePage {
    val items = ArrayList<FeedItem>()
    var continuation: String? = null

    fun walk(node: JsonElement?) {
        when (node) {
            is JsonObject -> {
                if ("adSlotRenderer" in node) return
                node["continuationItemRenderer"].objectOrNull()?.accountContinuationToken()?.let {
                    continuation = it
                    return
                }
                node.toFeedItem(FeedItemOwner())?.let {
                    items += it
                    return
                }
                node.values.forEach(::walk)
            }

            is JsonArray -> node.forEach(::walk)

            else -> Unit
        }
    }

    walk(this)
    return AccountBrowsePage(items.distinctBy { it.distinctKey() }, continuation)
}

private fun JsonObject.accountContinuationToken(): String? =
    this["continuationEndpoint"]
        .objectOrNull()
        ?.get("continuationCommand")
        .objectOrNull()
        ?.get("token")
        .stringOrNull()
        ?.takeIf(String::isNotBlank)
