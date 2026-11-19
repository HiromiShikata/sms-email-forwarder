package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.domain.buildUpdateNotificationText
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier

class AndroidAppUpdateNotifier(
    private val context: Context,
    private val tapIntent: Intent,
) : AppUpdateNotifier {

    override fun notifyReadyToInstall(releases: List<AppRelease>) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val newest = releases.firstOrNull() ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notes = buildUpdateNotificationText(releases)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.app_update_notification_title))
            .setContentText(context.getString(R.string.app_update_notification_body, newest.version))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notes))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
