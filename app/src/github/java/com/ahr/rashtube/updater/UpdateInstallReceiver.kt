package com.ahr.rashtube.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.ahr.rashtube.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives the installer session's result: shows the system confirmation, or records a failure. */
@AndroidEntryPoint
class UpdateInstallReceiver : BroadcastReceiver() {
    @Inject
    lateinit var installer: AppUpdateInstaller

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let(installer::offerConfirmation)
            }

            // Dismissing the system's prompt is a choice, not a failure: the page just offers Install again.
            PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Unit
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w(TAG, "Update install failed ($status): $message")
                Toast.makeText(context, message ?: context.getString(R.string.update_failed_install), Toast.LENGTH_LONG).show()
                installer.reportInstallFailure()
            }
        }
    }

    private companion object {
        const val TAG = "UpdateInstallReceiver"
    }
}
