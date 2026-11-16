package com.ahr.rashtube.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ahr.rashtube.R
import com.ahr.rashtube.data.engagement.VideoFeedbackUseCase
import com.ahr.rashtube.data.model.Video
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling notification action clicks
 */
class NotificationActionReceiver : BroadcastReceiver() {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun videoFeedback(): VideoFeedbackUseCase
    }

    companion object {
        const val ACTION_WATCH_LATER = "com.ahr.rashtube.action.WATCH_LATER"
        const val ACTION_MARK_WATCHED = "com.ahr.rashtube.action.MARK_WATCHED"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_THUMBNAIL_URL = "thumbnail_url"
        const val ACTION_CANCEL_DOWNLOAD = "com.ahr.rashtube.action.CANCEL_DOWNLOAD"
        const val ACTION_RETRY_DOWNLOAD = "com.ahr.rashtube.action.RETRY_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.ahr.rashtube.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.ahr.rashtube.action.RESUME_DOWNLOAD"
        const val ACTION_DISMISS_NOTIFICATION = "com.ahr.rashtube.action.DISMISS_NOTIFICATION"
        
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationAction", "Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_CANCEL_DOWNLOAD -> handleCancelDownload(context, intent)
            ACTION_RETRY_DOWNLOAD -> handleRetryDownload(context, intent)
            ACTION_PAUSE_DOWNLOAD -> handlePauseDownload(context, intent)
            ACTION_RESUME_DOWNLOAD -> handleResumeDownload(context, intent)
            ACTION_DISMISS_NOTIFICATION -> handleDismissNotification(context, intent)
            ACTION_WATCH_LATER, ACTION_MARK_WATCHED -> handleVideoAction(context, intent)
        }
    }

    /** "Watch later" and "Mark as watched" straight from a new-video notification, without opening the app. */
    private fun handleVideoAction(
        context: Context,
        intent: Intent,
    ) {
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return
        val action = intent.action
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val video =
            Video(
                id = videoId,
                title = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty(),
                channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME).orEmpty(),
                channelId = intent.getStringExtra(EXTRA_CHANNEL_ID).orEmpty(),
                thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL).orEmpty(),
                duration = 0,
                viewCount = 0,
                uploadDate = "",
                timestamp = System.currentTimeMillis(),
            )
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val feedback = EntryPointAccessors.fromApplication(appContext, Dependencies::class.java).videoFeedback()
                val toast =
                    if (action == ACTION_WATCH_LATER) {
                        feedback.saveToWatchLater(video)
                        R.string.saved
                    } else {
                        feedback.markWatched(video)
                        R.string.mark_as_watched_toast
                    }
                Handler(Looper.getMainLooper()).post { Toast.makeText(appContext, toast, Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                Log.e("NotificationAction", "Video action $action failed", e)
            } finally {
                if (notificationId != -1) NotificationHelper.cancelNotification(appContext, notificationId)
                pending.finish()
            }
        }
    }
    
    private fun handleCancelDownload(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
        
        // Cancel the notification
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }
        
        // Cancel the actual download task in system DownloadManager
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            downloadManager.remove(downloadId)
            Log.d("NotificationAction", "Download cancelled in system DownloadManager: $downloadId")
        }
    }
    
    private fun handleRetryDownload(context: Context, intent: Intent) {
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        
        // TODO: Retry the download
        // This would integrate with your download manager
        Log.d("NotificationAction", "Retrying download for: $videoTitle (ID: $videoId)")
    }
    
    private fun handlePauseDownload(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
        
        // TODO: Pause the download
        Log.d("NotificationAction", "Pausing download: $downloadId")
    }
    
    private fun handleResumeDownload(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
        
        // TODO: Resume the download
        Log.d("NotificationAction", "Resuming download: $downloadId")
    }
    
    private fun handleDismissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }
    }
}
