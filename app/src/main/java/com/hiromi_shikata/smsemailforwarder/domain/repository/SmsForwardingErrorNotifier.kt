package com.hiromi_shikata.smsemailforwarder.domain.repository

interface SmsForwardingErrorNotifier {
    fun notify(sender: String, errorMessage: String)
}
