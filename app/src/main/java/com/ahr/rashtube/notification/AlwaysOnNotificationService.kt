package com.ahr.rashtube.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ahr.rashtube.R
import com.ahr.rashtube.data.local.PlayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Keeps the app process alive with a quiet status notification, and asks for a new-video check every
 * few minutes. Without it Android may hold background checks back for hours on battery-saving ROMs.
 */
class AlwaysOnNotificationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (loop?.isActive != true) {
            loop =
                scope.launch {
                    while (isActive) {
                        runCatching { SubscriptionCheckWorker.runImmediateCheck(applicationContext) }
                        delay(CHECK_EVERY_MS)
                    }
                }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notif_always_on_channel), NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val open =
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_always_on_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "always_on_channel"
        private const val NOTIFICATION_ID = 7410
        private const val CHECK_EVERY_MS = 2 * 60 * 1000L

        /** Starts or stops the service. Android may refuse a start from the background; that is not an error. */
        fun setRunning(
            context: Context,
            running: Boolean,
        ) {
            val intent = Intent(context, AlwaysOnNotificationService::class.java)
            runCatching {
                if (running) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
            }
        }

        /** Starts the service when the switches ask for it; used on app open and after boot. */
        fun ensureRunning(context: Context) {
            val preferences = PlayerPreferences(context)
            val wanted =
                runBlocking {
                    preferences.notificationsEnabled.first() &&
                        preferences.notifNewVideosEnabled.first() &&
                        preferences.alwaysOnNotifications.first()
                }
            if (wanted) setRunning(context, true)
        }
    }
}

/** Restarts the always-on check after the phone boots or the app is updated. */
class AlwaysOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        AlwaysOnNotificationService.ensureRunning(context)
    }
}
