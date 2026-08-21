package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingSetupNotifier

class AndroidSmsForwardingSetupNotifier(private val context: Context) : SmsForwardingSetupNotifier {

    override fun notify(sender: String) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
            ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.setup_required_notification_title))
            .setContentText(context.getString(R.string.setup_required_notification_body, sender))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.setup_required_notification_body, sender)),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "setup_required"
        private const val NOTIFICATION_ID = 2000

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.setup_required_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.setup_required_channel_description)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
