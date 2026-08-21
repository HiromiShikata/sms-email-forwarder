package com.hiromi_shikata.smsemailforwarder

import android.app.Application
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingSetupNotifier

class SmsEmailForwarderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSmsForwardingErrorNotifier.createChannel(this)
        AndroidSmsForwardingSetupNotifier.createChannel(this)
    }
}

