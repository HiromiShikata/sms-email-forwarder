package com.hiromi_shikata.smsemailforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingSetupNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SmtpPasswordUnavailableException
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.entity.SMS_FORWARD_MAX_ATTEMPTS
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsForwardAttemptOutcome
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingSetupNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase
import java.util.concurrent.TimeUnit

private const val SMS_FORWARD_RETRY_INITIAL_DELAY_SECONDS = 30L

internal fun forwardWithNotification(
    useCase: SmsForwardUseCase,
    message: SmsMessage,
    notifier: SmsForwardingErrorNotifier,
    logRepository: ForwardingLogRepository,
    attemptNumber: Int = 1,
): SmsForwardAttemptOutcome {
    try {
        useCase.execute(message)
        logRepository.save(
            ForwardingLogEntry(
                timestamp = message.timestamp,
                sender = message.sender,
                status = ForwardingLogEntryStatus.FORWARDED,
                errorMessage = null,
            ),
        )
        return SmsForwardAttemptOutcome.FORWARDED
    } catch (e: Exception) {
        if (e is TransientEmailSendException && attemptNumber < SMS_FORWARD_MAX_ATTEMPTS) {
            logRepository.save(
                ForwardingLogEntry(
                    timestamp = message.timestamp,
                    sender = message.sender,
                    status = ForwardingLogEntryStatus.RETRYING,
                    errorMessage = e.message,
                ),
            )
            return SmsForwardAttemptOutcome.RETRY_SCHEDULED
        }
        notifier.notify(message.sender, e.message ?: "Unknown error")
        logRepository.save(
            ForwardingLogEntry(
                timestamp = message.timestamp,
                sender = message.sender,
                status = ForwardingLogEntryStatus.FAILED,
                errorMessage = e.message,
            ),
        )
        return SmsForwardAttemptOutcome.FAILED
    }
}

internal fun dispatchSms(
    sender: String,
    body: String,
    timestamp: Long,
    config: ForwardingConfig,
    setupNotifier: SmsForwardingSetupNotifier,
    logRepository: ForwardingLogRepository,
    enqueue: (sender: String, body: String, timestamp: Long) -> Unit,
) {
    if (!config.isComplete) {
        setupNotifier.notify(sender)
        logRepository.save(
            ForwardingLogEntry(
                timestamp = timestamp,
                sender = sender,
                status = ForwardingLogEntryStatus.SETUP_INCOMPLETE,
                errorMessage = null,
            ),
        )
        return
    }
    enqueue(sender, body, timestamp)
}

internal fun buildSmsForwardWorkRequest(
    sender: String,
    body: String,
    timestamp: Long,
): OneTimeWorkRequest {
    val data = Data.Builder()
        .putString(SmsForwardWorker.KEY_SENDER, sender)
        .putString(SmsForwardWorker.KEY_BODY, body)
        .putLong(SmsForwardWorker.KEY_TIMESTAMP, timestamp)
        .build()
    return OneTimeWorkRequestBuilder<SmsForwardWorker>()
        .setInputData(data)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, SMS_FORWARD_RETRY_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val logRepository = SharedPrefsForwardingLogRepository.create(context)
        val config = try {
            SharedPrefsForwardingConfigRepository(context).get()
        } catch (e: SmtpPasswordUnavailableException) {
            val errorNotifier = AndroidSmsForwardingErrorNotifier(context)
            reportSmtpPasswordUnavailable(senders = messages.map { it.originatingAddress ?: "Unknown" }, errorMessage = e.message ?: "Unknown error", timestamp = System.currentTimeMillis(), notifyError = { sender, message -> errorNotifier.notify(sender, message) }, saveLog = { entry -> logRepository.save(entry) })
            return
        }
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
                logRepository = logRepository,
            ) { s, b, t ->
                workManager.enqueue(buildSmsForwardWorkRequest(s, b, t))
            }
        }
    }
}
