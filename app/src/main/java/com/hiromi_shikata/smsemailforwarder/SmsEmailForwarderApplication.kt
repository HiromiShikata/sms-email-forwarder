package com.hiromi_shikata.smsemailforwarder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidAppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingInProgressNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingSetupNotifier
import java.util.concurrent.TimeUnit

class SmsEmailForwarderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSmsForwardingErrorNotifier.createChannel(this)
        AndroidSmsForwardingInProgressNotifier.createChannel(this)
        AndroidSmsForwardingSetupNotifier.createChannel(this)
        AndroidAppUpdateNotifier.createChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "app_update_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(1, TimeUnit.HOURS).build(),
        )
    }
}

