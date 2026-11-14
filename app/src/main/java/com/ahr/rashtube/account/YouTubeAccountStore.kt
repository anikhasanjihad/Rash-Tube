/*
 * Rash Tube — YouTube account storage.
 * Part of Rash Tube by AHR, a modified version of Flow (A-EDev), licensed under GPL-3.0.
 */
package com.ahr.rashtube.account

import android.content.Context
import android.webkit.CookieManager
import com.ahr.rashtube.innertube.YouTube
import com.ahr.rashtube.innertube.models.AccountInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class YouTubeAccountState(
    val cookie: String? = null,
    val dataSyncId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val handle: String? = null,
    val avatarUrl: String? = null,
) {
    /** Signed in means the session cookie carries SAPISID, which InnerTube needs to sign requests. */
    val signedIn: Boolean get() = cookie?.split("; ")?.any { it.startsWith("SAPISID=") } == true
}

/**
 * Keeps the signed-in YouTube session. Stored in a private preferences file that is not part of
 * any backup rule, so the cookie never leaves the device.
 */
class YouTubeAccountStore private constructor(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<YouTubeAccountState> = _state.asStateFlow()

    private fun read() =
        YouTubeAccountState(
            cookie = prefs.getString(KEY_COOKIE, null),
            dataSyncId = prefs.getString(KEY_DATA_SYNC_ID, null),
            name = prefs.getString(KEY_NAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            handle = prefs.getString(KEY_HANDLE, null),
            avatarUrl = prefs.getString(KEY_AVATAR, null),
        )

    /** Pushes the stored session into the InnerTube client. */
    fun applyToClient() {
        val current = _state.value
        if (current.signedIn) {
            YouTube.cookie = current.cookie
            YouTube.dataSyncId = current.dataSyncId
            YouTube.useLoginForBrowse = true
        } else {
            YouTube.cookie = null
            YouTube.dataSyncId = null
            YouTube.useLoginForBrowse = false
        }
    }

    fun saveLogin(
        cookie: String,
        dataSyncId: String?,
    ) {
        prefs
            .edit()
            .putString(KEY_COOKIE, cookie)
            .putString(KEY_DATA_SYNC_ID, dataSyncId?.substringBefore("||")?.takeIf { it.isNotBlank() })
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_HANDLE)
            .remove(KEY_AVATAR)
            .apply()
        _state.value = read()
        applyToClient()
    }

    fun saveProfile(info: AccountInfo) {
        prefs
            .edit()
            .putString(KEY_NAME, info.name)
            .putString(KEY_EMAIL, info.email)
            .putString(KEY_HANDLE, info.channelHandle)
            .putString(KEY_AVATAR, info.thumbnailUrl)
            .apply()
        _state.value = read()
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _state.value = read()
        applyToClient()
        CookieManager.getInstance().removeAllCookies(null)
    }

    companion object {
        private const val PREFS = "yt_account_session"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_DATA_SYNC_ID = "data_sync_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_HANDLE = "handle"
        private const val KEY_AVATAR = "avatar"

        @Volatile private var instance: YouTubeAccountStore? = null

        @Volatile internal var appContext: Context? = null
            private set

        fun getInstance(context: Context): YouTubeAccountStore =
            instance ?: synchronized(this) {
                instance ?: YouTubeAccountStore(context.applicationContext).also {
                    instance = it
                    appContext = context.applicationContext
                }
            }

        /** The store if the app has already created it, for call sites that have no [Context]. */
        fun peek(): YouTubeAccountStore? = instance
    }
}
