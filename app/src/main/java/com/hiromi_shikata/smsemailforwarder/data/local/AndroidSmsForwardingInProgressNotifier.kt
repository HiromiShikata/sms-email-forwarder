package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hiromi_shikata.smsemailforwarder.R

class AndroidSmsForwardingInProgressNotifier {
    companion object {
        const val CHANNEL_ID = "sms_forwarding"
        const val NOTIFICATION_ID = 3000

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.forwarding_in_progress_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.forwarding_in_progress_channel_description)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }

        fun buildNotification(context: Context) =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(context.getString(R.string.forwarding_in_progress_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
    }
}
