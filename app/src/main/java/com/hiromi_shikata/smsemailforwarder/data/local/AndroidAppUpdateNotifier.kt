package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.domain.AppUpdateNotificationComposer
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import java.io.File

class AndroidAppUpdateNotifier(private val context: Context) : AppUpdateNotifier {

    override fun notifyDownloaded(latestVersion: String, releases: List<AppRelease>, apkFile: File) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(AppUpdateNotificationComposer.title(releases))
            .setContentText(context.getString(R.string.app_update_notification_body, latestVersion))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(AppUpdateNotificationComposer.bigText(releases)),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun cancel() {
        context.getSystemService<NotificationManager>()?.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "app_update"
        const val NOTIFICATION_ID = 4000
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_update_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.app_update_channel_description)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
