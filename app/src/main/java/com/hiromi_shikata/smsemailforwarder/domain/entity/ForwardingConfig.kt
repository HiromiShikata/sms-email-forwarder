package com.hiromi_shikata.smsemailforwarder.domain.entity

data class ForwardingConfig(
    val destinationEmail: String,
    val authMode: EmailAuthMode,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUsername: String,
    val smtpPassword: String,
    val apiEndpointUrl: String = "",
    val apiKey: String = "",
) {
    val isComplete: Boolean
        get() = when (authMode) {
            EmailAuthMode.SMTP -> destinationEmail.isNotBlank() &&
                smtpHost.isNotBlank() &&
                smtpPort > 0 &&
                smtpUsername.isNotBlank() &&
                smtpPassword.isNotBlank()
            EmailAuthMode.GOOGLE_ACCOUNT -> destinationEmail.isNotBlank() &&
                smtpUsername.isNotBlank() &&
                smtpPassword.isNotBlank()
            EmailAuthMode.HTTPS_API -> isHttpsApiEndpointUrl(apiEndpointUrl) && apiKey.isNotBlank()
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
