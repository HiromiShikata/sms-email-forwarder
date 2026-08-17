package com.hiromi_shikata.smsemailforwarder.domain.entity

data class ForwardingConfig(
    val destinationEmail: String,
    val authMode: EmailAuthMode,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUsername: String,
    val smtpPassword: String,
) {
    val isComplete: Boolean
        get() = destinationEmail.isNotBlank() && when (authMode) {
            EmailAuthMode.SMTP -> smtpHost.isNotBlank() &&
                smtpPort > 0 &&
                smtpUsername.isNotBlank() &&
                smtpPassword.isNotBlank()
            EmailAuthMode.GOOGLE_ACCOUNT -> smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
        }

    companion object {
        val EMPTY = ForwardingConfig(
            destinationEmail = "",
            authMode = EmailAuthMode.GOOGLE_ACCOUNT,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpUsername = "",
            smtpPassword = "",
        )
    }
}
