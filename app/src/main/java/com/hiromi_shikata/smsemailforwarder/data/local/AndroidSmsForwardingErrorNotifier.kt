package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import java.util.concurrent.atomic.AtomicInteger

class AndroidSmsForwardingErrorNotifier(private val context: Context) : SmsForwardingErrorNotifier {

    override fun notify(sender: String, errorMessage: String) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.forwarding_error_title))
            .setContentText(context.getString(R.string.forwarding_error_body, sender, errorMessage))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.forwarding_error_body, sender, errorMessage)),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(notificationId.getAndIncrement(), notification)
    }

    companion object {
        const val CHANNEL_ID = "forwarding_error"
        private val notificationId = AtomicInteger(1000)

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.forwarding_error_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.forwarding_error_channel_description)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}
