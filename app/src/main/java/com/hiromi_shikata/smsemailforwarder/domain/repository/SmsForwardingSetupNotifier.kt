package com.hiromi_shikata.smsemailforwarder.domain.repository

interface SmsForwardingSetupNotifier {
    fun notify(sender: String)
}
