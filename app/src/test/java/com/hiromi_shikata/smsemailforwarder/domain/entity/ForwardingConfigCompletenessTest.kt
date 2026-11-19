package com.hiromi_shikata.smsemailforwarder.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardingConfigCompletenessTest {
    @Test
    fun `isComplete is true only when every field required by the auth mode is set`() {
        val cases = listOf(
            CompletenessCase("HTTPS API complete", httpsConfig("https://example.com/send", "key-1"), true),
            CompletenessCase("HTTPS API without destination email", httpsConfig("https://example.com/send", "key-1").copy(destinationEmail = ""), true),
            CompletenessCase("HTTPS API with http endpoint", httpsConfig("http://example.com/send", "key-1"), false),
            CompletenessCase("HTTPS API with blank endpoint", httpsConfig("", "key-1"), false),
            CompletenessCase("HTTPS API with blank key", httpsConfig("https://example.com/send", " "), false),
            CompletenessCase("SMTP complete", smtpConfig(EmailAuthMode.SMTP), true),
            CompletenessCase("SMTP without destination email", smtpConfig(EmailAuthMode.SMTP).copy(destinationEmail = ""), false),
            CompletenessCase("GOOGLE_ACCOUNT complete", smtpConfig(EmailAuthMode.GOOGLE_ACCOUNT), true),
            CompletenessCase("GOOGLE_ACCOUNT without password", smtpConfig(EmailAuthMode.GOOGLE_ACCOUNT).copy(smtpPassword = ""), false),
        )
        for (case in cases) {
            assertEquals(case.label, case.expected, case.config.isComplete)
        }
    }

    @Test
    fun `isHttpsApiEndpointUrl accepts only absolute https URLs with a host`() {
        val cases = listOf(
            EndpointCase("https://example.com/send", true),
            EndpointCase("HTTPS://example.com/send", true),
            EndpointCase("https://example.com:8443/send?x=1", true),
            EndpointCase("http://example.com/send", false),
            EndpointCase("ftp://example.com/send", false),
            EndpointCase("https://", false),
            EndpointCase("example.com/send", false),
            EndpointCase("", false),
            EndpointCase("not a url", false),
        )
        for (case in cases) {
            assertEquals(case.url, case.expected, isHttpsApiEndpointUrl(case.url))
        }
    }

    private fun httpsConfig(endpoint: String, apiKey: String) = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.HTTPS_API,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "",
        smtpPassword = "",
        apiEndpointUrl = endpoint,
        apiKey = apiKey,
    )

    private fun smtpConfig(mode: EmailAuthMode) = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = mode,
        smtpHost = "smtp.example.net",
        smtpPort = 587,
        smtpUsername = "sender@example.net",
        smtpPassword = "secret",
    )
}

private data class CompletenessCase(val label: String, val config: ForwardingConfig, val expected: Boolean)

private data class EndpointCase(val url: String, val expected: Boolean)
