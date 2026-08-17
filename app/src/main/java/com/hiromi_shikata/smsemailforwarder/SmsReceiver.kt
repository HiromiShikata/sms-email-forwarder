package com.hiromi_shikata.smsemailforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.SmtpEmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase
import kotlin.concurrent.thread

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

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        thread {
            try {
                val configRepository = SharedPrefsForwardingConfigRepository(context)
                val emailSendRepository = SmtpEmailSendRepository()
                val notifier = AndroidSmsForwardingErrorNotifier(context)
                messages.groupBy { it.originatingAddress }.forEach { (sender, parts) ->
                    val body = parts.joinToString("") { it.messageBody }
                    val smsMessage = SmsMessage(
                        sender = sender ?: "Unknown",
                        body = body,
                        timestamp = System.currentTimeMillis(),
                    )
                    forwardWithNotification(
                        SmsForwardUseCase(configRepository, emailSendRepository),
                        smsMessage,
                        notifier,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
