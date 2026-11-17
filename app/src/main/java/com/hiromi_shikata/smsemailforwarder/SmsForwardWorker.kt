package com.hiromi_shikata.smsemailforwarder

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingInProgressNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.SmtpEmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase

internal fun getForegroundServiceTypeForSdk(sdkInt: Int): Int? =
    if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    } else {
        null
    }

class SmsForwardWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun getForegroundInfo(): ForegroundInfo {
        val notification = AndroidSmsForwardingInProgressNotifier.buildNotification(applicationContext)
        val serviceType = getForegroundServiceTypeForSdk(Build.VERSION.SDK_INT)
        return if (serviceType != null) {
            ForegroundInfo(
                AndroidSmsForwardingInProgressNotifier.NOTIFICATION_ID,
                notification,
                serviceType,
            )
        } else {
            ForegroundInfo(
                AndroidSmsForwardingInProgressNotifier.NOTIFICATION_ID,
                notification,
            )
        }
    }

    override fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
        val configRepository = SharedPrefsForwardingConfigRepository(applicationContext)
        val emailSendRepository = SmtpEmailSendRepository()
        val notifier = AndroidSmsForwardingErrorNotifier(applicationContext)
        val logRepository = SharedPrefsForwardingLogRepository.create(applicationContext)
        forwardWithNotification(
            SmsForwardUseCase(configRepository, emailSendRepository),
            SmsMessage(sender = sender, body = body, timestamp = timestamp),
            notifier,
            logRepository,
        )
        return Result.success()
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
