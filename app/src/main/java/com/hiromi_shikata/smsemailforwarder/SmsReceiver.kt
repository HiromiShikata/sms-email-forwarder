package com.hiromi_shikata.smsemailforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingSetupNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingSetupNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase

internal fun forwardWithNotification(
    useCase: SmsForwardUseCase,
    message: SmsMessage,
    notifier: SmsForwardingErrorNotifier,
) {
    try {
        useCase.execute(message)
    } catch (e: Exception) {
        notifier.notify(message.sender, e.message ?: "Unknown error")
    }
}

internal fun dispatchSms(
    sender: String,
    body: String,
    timestamp: Long,
    config: ForwardingConfig,
    setupNotifier: SmsForwardingSetupNotifier,
    enqueue: (sender: String, body: String, timestamp: Long) -> Unit,
) {
    if (!config.isComplete) {
        setupNotifier.notify(sender)
        return
    }
    enqueue(sender, body, timestamp)
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val config = SharedPrefsForwardingConfigRepository(context).get()
        val setupNotifier = AndroidSmsForwardingSetupNotifier(context)
        val workManager = WorkManager.getInstance(context)
        messages.groupBy { it.originatingAddress }.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody }
            dispatchSms(
                sender = sender ?: "Unknown",
                body = body,
                timestamp = System.currentTimeMillis(),
                config = config,
                setupNotifier = setupNotifier,
            ) { s, b, t ->
                val data = Data.Builder()
                    .putString(SmsForwardWorker.KEY_SENDER, s)
                    .putString(SmsForwardWorker.KEY_BODY, b)
                    .putLong(SmsForwardWorker.KEY_TIMESTAMP, t)
                    .build()
                workManager.enqueue(
                    OneTimeWorkRequestBuilder<SmsForwardWorker>()
                        .setInputData(data)
                        .build(),
                )
            }
        }
    }
}
