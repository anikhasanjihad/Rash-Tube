/*
 * Rash Tube — YouTube sign-in and account screen.
 * Part of Rash Tube by AHR, a modified version of Flow (A-EDev), licensed under GPL-3.0.
 */
package com.ahr.rashtube.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import dagger.hilt.android.AndroidEntryPoint
import com.ahr.rashtube.R
import com.ahr.rashtube.data.local.AppFontPreferences
import com.ahr.rashtube.data.local.LocalDataManager
import com.ahr.rashtube.innertube.YouTube
import com.ahr.rashtube.ui.startup.RashTheme
import com.ahr.rashtube.ui.startup.themeSettings
import com.ahr.rashtube.ui.theme.liquidGlass
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject

private const val LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
        "&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"
private const val MUSIC_HOST = "https://music.youtube.com"
private const val READ_CONFIG_JS =
    "(function(){try{var c=(window.yt&&window.yt.config_)||{};" +
        "return JSON.stringify({v:c.VISITOR_DATA||'',d:c.DATASYNC_ID||''});}catch(e){return '{}';}})()"

@AndroidEntryPoint
class YouTubeAccountActivity : ComponentActivity() {
    @Inject
    lateinit var appFontPreferences: AppFontPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val theme = LocalDataManager(applicationContext).themeSettings(appFontPreferences)
        setContent {
            val settings = theme.collectAsState(initial = null).value ?: return@setContent
            RashTheme(settings) { AccountRoot(onClose = ::finish) }
        }
    }

    companion object {
        /** Opens the screen from a place that has no [Context] of its own. */
        fun launch(context: Context? = null) {
            val appContext = context ?: YouTubeAccountStore.appContext ?: return
            appContext.startActivity(Intent(appContext, YouTubeAccountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun AccountRoot(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { YouTubeAccountStore.getInstance(context) }
    val state by store.state.collectAsState()
    var loggingIn by remember { mutableStateOf(false) }

    if (loggingIn) {
        YouTubeLoginWebView(store = store, onFinished = { loggingIn = false })
    } else {
        AccountScreen(state = state, store = store, onLogin = { loggingIn = true }, onClose = onClose)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeLoginWebView(
    store: YouTubeAccountStore,
    onFinished: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler { if (webView?.canGoBack() == true) webView?.goBack() else onFinished() }

    AndroidView(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        factory = { context ->
            var finished = false
            WebView(context).apply {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Google refuses sign-in inside an embedded WebView that announces itself as one.
                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            if (finished || url == null || !url.startsWith(MUSIC_HOST)) return
                            val cookie = CookieManager.getInstance().getCookie(url) ?: return
                            if (cookie.split("; ").none { it.startsWith("SAPISID=") }) return
                            view.evaluateJavascript(READ_CONFIG_JS) { raw ->
                                if (finished) return@evaluateJavascript
                                val config =
                                    runCatching { JSONObject(JSONTokener(raw).nextValue() as String) }.getOrNull()
                                finished = true
                                config?.optString("v")?.takeIf { it.isNotBlank() }?.let { YouTube.visitorData = it }
                                store.saveLogin(cookie, config?.optString("d"))
                                onFinished()
                            }
                        }
                    }
                webView = this
                loadUrl(LOGIN_URL)
            }
        },
        onRelease = { it.destroy() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountScreen(
    state: YouTubeAccountState,
    store: YouTubeAccountStore,
    onLogin: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val importDone = stringResource(R.string.yt_account_import_done)
    val importFailed = stringResource(R.string.yt_account_import_failed)

    LaunchedEffect(state.signedIn, state.name) {
        if (state.signedIn && state.name == null) {
            YouTube.accountInfo().onSuccess { store.saveProfile(it) }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.yt_account_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.signedIn) {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.yt_account_signed_out_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.yt_account_signed_out_body), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.yt_account_sign_in)) }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().liquidGlass().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                    )
                    Column {
                        Text(state.name ?: stringResource(R.string.yt_account_loading), style = MaterialTheme.typography.titleMedium)
                        state.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        state.handle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            importing = true
                            message = null
                            message =
                                YouTubeSubscriptionImporter
                                    .import(context)
                                    .fold(onSuccess = { String.format(importDone, it) }, onFailure = { importFailed })
                            importing = false
                        }
                    },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (importing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(stringResource(R.string.yt_account_import))
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                OutlinedButton(onClick = { store.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.yt_account_sign_out))
                }
            }
        }
    }
}
