package com.hiromi_shikata.smsemailforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.AccountManagerOAuthTokenProvider
import com.hiromi_shikata.smsemailforwarder.data.remote.GmailApiEmailSendRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.SmtpEmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        thread {
            try {
                val configRepository = SharedPrefsForwardingConfigRepository(context)
                val config = configRepository.get()
                val emailSendRepository: EmailSendRepository = when (config.authMode) {
                    EmailAuthMode.SMTP -> SmtpEmailSendRepository()
                    EmailAuthMode.GOOGLE_ACCOUNT -> GmailApiEmailSendRepository(
                        AccountManagerOAuthTokenProvider(context),
                    )
                }
                messages.groupBy { it.originatingAddress }.forEach { (sender, parts) ->
                    val body = parts.joinToString("") { it.messageBody }
                    val smsMessage = SmsMessage(
                        sender = sender ?: "Unknown",
                        body = body,
                        timestamp = System.currentTimeMillis(),
                    )
                    SmsForwardUseCase(
                        configRepository = configRepository,
                        emailSendRepository = emailSendRepository,
                    ).execute(smsMessage)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
