package com.hiromi_shikata.smsemailforwarder

import android.app.Application
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier

class SmsEmailForwarderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSmsForwardingErrorNotifier.createChannel(this)
    }
}

