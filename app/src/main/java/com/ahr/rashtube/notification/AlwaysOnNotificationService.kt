package com.ahr.rashtube.notification

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ahr.rashtube.data.local.PlayerPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Legacy foreground service of earlier versions. A foreground service must show a notification, so it is
 * gone: the frequent new-video check now runs through WorkManager ([AlwaysOnChainWorker]), which shows
 * nothing. This class only stops itself if an old install still starts it.
 */
class AlwaysOnNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        private const val OLD_CHANNEL_ID = "always_on_channel"
        private const val OLD_SILENT_CHANNEL_ID = "always_on_silent_channel"

        /** Turns the silent frequent check on or off, and makes sure no leftover service notification stays. */
        fun setRunning(
            context: Context,
            running: Boolean,
        ) {
            runCatching { context.stopService(Intent(context, AlwaysOnNotificationService::class.java)) }
            runCatching {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.cancel(LEGACY_NOTIFICATION_ID)
                manager.deleteNotificationChannel(OLD_CHANNEL_ID)
                manager.deleteNotificationChannel(OLD_SILENT_CHANNEL_ID)
            }
            if (running) {
                AlwaysOnChainWorker.start(context)
            } else {
                WorkManager.getInstance(context).cancelUniqueWork(AlwaysOnChainWorker.NAME)
            }
        }

        /** Starts the check when the switches ask for it; used on app open and after boot. */
        fun ensureRunning(context: Context) {
            val preferences = PlayerPreferences(context)
            val wanted =
                runBlocking {
                    preferences.notificationsEnabled.first() &&
                        preferences.notifNewVideosEnabled.first() &&
                        preferences.alwaysOnNotifications.first()
                }
            setRunning(context, wanted)
        }

        private const val LEGACY_NOTIFICATION_ID = 7410
    }
}

/**
 * Asks for a new-video check, then schedules itself again a few minutes later. A chain of delayed
 * WorkManager jobs, so nothing is shown to the user and no foreground service is needed.
 */
class AlwaysOnChainWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = PlayerPreferences(applicationContext)
        val wanted =
            preferences.notificationsEnabled.first() &&
                preferences.notifNewVideosEnabled.first() &&
                preferences.alwaysOnNotifications.first()
        if (!wanted) return Result.success()
        runCatching { SubscriptionCheckWorker.runImmediateCheck(applicationContext) }
        enqueue(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        const val NAME = "always_on_chain"
        private const val CHECK_EVERY_MINUTES = 3L

        /** Begins the chain unless one is already waiting. */
        fun start(context: Context) = enqueue(context, ExistingWorkPolicy.KEEP)

        private fun enqueue(
            context: Context,
            policy: ExistingWorkPolicy,
        ) {
            val request =
                OneTimeWorkRequestBuilder<AlwaysOnChainWorker>()
                    .setInitialDelay(CHECK_EVERY_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, policy, request)
        }
    }
}

/** Restarts the silent check after the phone boots or the app is updated. */
class AlwaysOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        AlwaysOnNotificationService.ensureRunning(context)
    }
}
