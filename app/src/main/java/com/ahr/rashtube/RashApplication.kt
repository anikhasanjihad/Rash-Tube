package com.ahr.rashtube

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import com.ahr.rashtube.applock.AppLock
import com.ahr.rashtube.data.contentfilter.aislist.AiListRepository
import com.ahr.rashtube.data.local.PlayerPreferences
import com.ahr.rashtube.data.local.SubscriptionRepository
import com.ahr.rashtube.data.profile.ActiveProfile
import com.ahr.rashtube.data.profile.PendingSlice
import com.ahr.rashtube.data.profile.PostSwitchWork
import com.ahr.rashtube.data.profile.ProfileFiles
import com.ahr.rashtube.data.profile.ProfileSlices
import com.ahr.rashtube.data.profile.ProfileWebView
import com.ahr.rashtube.data.profile.ProfileWorkReconciler
import com.ahr.rashtube.data.recommendation.RashNeuroEngine
import com.ahr.rashtube.data.repository.NewPipeDownloader
import com.ahr.rashtube.data.repository.YouTubeRepository
import com.ahr.rashtube.discord.DiscordPresenceRuntime
import com.ahr.rashtube.innertube.YouTube
import com.ahr.rashtube.network.AppProxyManager
import com.ahr.rashtube.network.VpnStateMonitor
import com.ahr.rashtube.notification.AutoBackupWorker
import com.ahr.rashtube.notification.BackgroundCheckScheduler
import com.ahr.rashtube.notification.BreakReminderTimer
import com.ahr.rashtube.notification.DailyLimitGuard
import com.ahr.rashtube.notification.NotificationHelper
import com.ahr.rashtube.player.PlaybackShutdown
import com.ahr.rashtube.player.SleepTimerManager
import com.ahr.rashtube.sync2.service.LinkedCatchUp
import com.ahr.rashtube.sync2.service.LinkedForeground
import com.ahr.rashtube.sync2.service.LinkedProfiles
import com.ahr.rashtube.sync2.service.LinkedSync
import com.ahr.rashtube.ui.screens.home.HomeFeedLanguageReset
import com.ahr.rashtube.utils.AppLanguageManager
import com.ahr.rashtube.utils.RashCrashHandler
import com.ahr.rashtube.utils.PerformanceDispatcher
import com.ahr.rashtube.utils.cipher.PipePipeNsigDecoder
import com.ahr.rashtube.utils.contentYouTubeLocale
import com.ahr.rashtube.utils.newPipeContentCountry
import com.ahr.rashtube.utils.newPipeLocalization
import com.ahr.rashtube.utils.potoken.NewPipePoTokenProvider
import com.ahr.rashtube.utils.potoken.VisitorIdentityStore
import com.ahr.rashtube.utils.potoken.WebPoTokenSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import java.security.Security
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class RashApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var vpnStateMonitor: VpnStateMonitor

    @Inject
    lateinit var visitorIdentityStore: VisitorIdentityStore

    @Inject
    internal lateinit var homeFeedLanguageReset: HomeFeedLanguageReset

    @Inject
    lateinit var profileWorkReconciler: ProfileWorkReconciler

    @Inject
    lateinit var postSwitchWork: PostSwitchWork

    @Inject
    lateinit var profileSlices: dagger.Lazy<ProfileSlices>

    @Inject
    lateinit var linkedForeground: LinkedForeground

    @Inject
    lateinit var breakReminderTimer: BreakReminderTimer

    @Inject
    lateinit var dailyLimitGuard: DailyLimitGuard

    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var linkedProfiles: dagger.Lazy<LinkedProfiles>

    @Inject
    lateinit var linkedSync: dagger.Lazy<LinkedSync>

    @Inject
    lateinit var aiList: AiListRepository

    @Inject
    lateinit var subscriptionRepository: com.ahr.rashtube.data.local.SubscriptionRepository

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    companion object {
        private const val TAG = "RashApplication"
        lateinit var appContext: Context
            private set
    }

    override fun attachBaseContext(base: Context) {
        ActiveProfile.attach(base)
        super.attachBaseContext(AppLanguageManager.wrapContext(base, AppLanguageManager.resolveLanguageTag(base)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        ProfileWebView.applySuffix(this)
        super.onCreate()
        appContext = applicationContext
        linkedForeground.install()
        breakReminderTimer.install()
        dailyLimitGuard.install()
        appLock.install()
        YouTube.cacheDirectory = cacheDir.resolve("innertube_http_cache")

        DiscordPresenceRuntime.initialize(this, okHttpClient)

        val playerPreferences = PlayerPreferences(this)

        // Injects modern TLS/SSL certificates so OkHttp and Ktor don't crash
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.N_MR1) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Install crash handler for real-time monitoring
        RashCrashHandler.install(this)

        try {
            // Seeded from the device locale so the very first extraction is already localized; the
            // stored app-language/region preference takes over as soon as it loads below.
            val localization = newPipeLocalization(Locale.getDefault().toLanguageTag())
            NewPipe.init(
                NewPipeDownloader.getInstance(this),
                localization,
                newPipeContentCountry(Locale.getDefault().country),
            )
            YoutubeStreamExtractor.setPoTokenProvider(NewPipePoTokenProvider)
            Log.d(TAG, "NewPipe initialized with ${localization.localizationCode}")
        } catch (e: Exception) {
            // Log error but don't crash the app
            Log.e(TAG, "Failed to initialize NewPipe", e)
        }

        try {
            com.ahr.rashtube.utils.cipher.CipherDeobfuscator
                .initialize(this)
            Log.d(TAG, "CipherDeobfuscator initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CipherDeobfuscator", e)
        }

        PipePipeNsigDecoder.initialize(this)

        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)
        Log.d(TAG, "Notification channels created")

        /*
        try {
            // Initialize YoutubeDL
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(this)
            Log.d(TAG, "YoutubeDL initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YoutubeDL", e)
        }
         */

        PendingSlice.start(ProfileFiles.noBackupRoot(this), CoroutineScope(SupervisorJob() + Dispatchers.IO)) { file ->
            profileSlices.get().applyPending(file)
        }
        LinkedCatchUp.start(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
            PendingSlice.await()
            if (linkedProfiles.get().catchingUp()) linkedSync.get().syncNow()
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            RashNeuroEngine.getInstance(this@RashApplication).aiListMatcher = aiList::currentMatcher
            launch { profileWorkReconciler.reconcile() }
            launch { AutoBackupWorker.keepScheduled(this@RashApplication) }
            launch { postSwitchWork.runIfSwitched() }
            if (!playerPreferences.allBellsOnMigrated.first()) {
                subscriptionRepository.enableNotificationsForAll()
                playerPreferences.setAllBellsOnMigrated()
            }
            com.ahr.rashtube.notification.SubscriptionCheckWorker.runImmediateCheck(this@RashApplication)
            BackgroundCheckScheduler.follow(this@RashApplication, playerPreferences)
        }
        SleepTimerManager.attachExitCallback { PlaybackShutdown.closeApp(this) }

        val warmups = StartupWarmups(this)
        warmups.startWithFirstActivity()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            playerPreferences.proxyConfig
                .distinctUntilChanged()
                .flatMapLatest { config ->
                    if (config.watchesVpn()) {
                        vpnStateMonitor.vpnActive().map { vpnActive -> config to vpnActive }
                    } else {
                        flowOf(config to false)
                    }
                }.collectLatest { (proxyConfig, vpnActive) ->
                    applyProxyConfig(proxyConfig, vpnActive)
                    warmups.onProxyApplied()
                }
        }

        YouTube.onVisitorDataChanged = visitorIdentityStore::save
        com.ahr.rashtube.account.YouTubeAccountStore.getInstance(this).applyToClient()
        WebPoTokenSession.bindIdentityStore(visitorIdentityStore)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                visitorIdentityStore.restore()?.let { restored ->
                    YouTube.visitorData = restored
                    Log.d(TAG, "visitorData restored")
                }
            } catch (e: Exception) {
                Log.w(TAG, "visitorData restore error: ${e.message}")
            }
            warmups.onIdentityRestored()
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppLanguageManager.appliedLanguage.filterNotNull().collect { tag ->
                if (playerPreferences.appLanguage.first() != tag) playerPreferences.setAppLanguage(tag)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            combine(
                AppLanguageManager.appliedLanguage.filterNotNull(),
                playerPreferences.contentLanguage,
                playerPreferences.trendingRegion,
            ) { appLanguage, contentLanguage, region ->
                contentYouTubeLocale(appLanguage, contentLanguage, region, AppLanguageManager.deviceLocale(this@RashApplication))
            }.distinctUntilChanged().collectLatest { newLocale ->
                YouTube.locale = newLocale
                NewPipe.setupLocalization(
                    newPipeLocalization(newLocale.hl),
                    ContentCountry(newLocale.gl),
                )
                Log.d(TAG, "Dynamic YouTube Locale updated: gl=${newLocale.gl}, hl=${newLocale.hl}")
                homeFeedLanguageReset.onContentLanguage(newLocale.hl)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            var lastRegion: String? = null
            playerPreferences.trendingRegion.collectLatest { region ->
                if (lastRegion != null && lastRegion != region) {
                    Log.d(TAG, "Trending region changed from $lastRegion to $region. Invalidate visitor data.")
                    YouTube.visitorData = null

                    YouTube
                        .visitorData()
                        .onSuccess { data ->
                            if (!data.isNullOrEmpty()) {
                                YouTube.visitorData = data
                                Log.d(TAG, "Fresh visitorData fetched for region: $region")
                            }
                        }.onFailure { e ->
                            Log.w(TAG, "Failed to fetch fresh visitorData: ${e.message}")
                        }
                }
                lastRegion = region
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = SubscriptionRepository.getInstance(this@RashApplication)
                val youtubeRepository = YouTubeRepository.getInstance(playerPreferences)
                val repaired =
                    repository.repairVideoThumbnailSubscriptions { channelId ->
                        // Startup's own fetches hold the InnerTube connections for a while; nothing waits on this.
                        withTimeoutOrNull(20_000L) {
                            youtubeRepository.fetchChannelAvatarById(channelId)
                        }.orEmpty()
                    }
                if (repaired > 0) {
                    Log.i(TAG, "Repaired $repaired subscription thumbnails")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Subscription thumbnail repair failed: ${e.message}")
            }
        }
    }

    private fun applyProxyConfig(
        config: com.ahr.rashtube.network.AppProxyConfig,
        vpnActive: Boolean,
    ) {
        AppProxyManager.update(config, vpnActive)
        YouTube.proxy = AppProxyManager.currentProxy()
        YouTube.proxyAuth = AppProxyManager.currentHttpProxyAuthorizationHeader()
    }

    override fun onTerminate() {
        DiscordPresenceRuntime.shutdown()
        super.onTerminate()
        // Clean up performance dispatcher resources
        PerformanceDispatcher.shutdown()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        RashCrashHandler.recordPhase("memory", "RashApplication.onLowMemory")
        releaseVolatileMemory()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        RashCrashHandler.recordPhase("memory", "RashApplication.onTrimMemory level=$level")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            releaseVolatileMemory()
        }
    }

    private fun releaseVolatileMemory() {
        if (::imageLoader.isInitialized) {
            imageLoader.memoryCache?.clear()
        }
        if (::okHttpClient.isInitialized) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                okHttpClient.connectionPool.evictAll()
            }
        }
    }
}
