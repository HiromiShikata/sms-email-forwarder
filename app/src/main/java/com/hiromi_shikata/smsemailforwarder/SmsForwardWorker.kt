package com.hiromi_shikata.smsemailforwarder

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingInProgressNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.SmtpEmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase

class SmsForwardWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(
            AndroidSmsForwardingInProgressNotifier.NOTIFICATION_ID,
            AndroidSmsForwardingInProgressNotifier.buildNotification(applicationContext),
        )

    override fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        val configRepository = SharedPrefsForwardingConfigRepository(applicationContext)
        val emailSendRepository = SmtpEmailSendRepository()
        val notifier = AndroidSmsForwardingErrorNotifier(applicationContext)
        forwardWithNotification(
            SmsForwardUseCase(configRepository, emailSendRepository),
            SmsMessage(sender = sender, body = body, timestamp = timestamp),
            notifier,
        )
        return Result.success()
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
