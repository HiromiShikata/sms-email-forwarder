package com.hiromi_shikata.smsemailforwarder.domain.entity

const val SMS_FORWARD_MAX_ATTEMPTS = 10

enum class SmsForwardAttemptOutcome {
    FORWARDED,
    RETRY_SCHEDULED,
    FAILED,
}
