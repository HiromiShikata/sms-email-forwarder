package com.hiromi_shikata.smsemailforwarder.domain.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingConfigTest {
    private val completeSmtpConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.SMTP,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "app-password",
        googleAccountName = "",
    )

    private val completeGoogleAccountConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = "",
        smtpPort = 0,
        smtpUsername = "",
        smtpPassword = "",
        googleAccountName = "user@gmail.com",
    )

    @Test
    fun `isComplete returns true for smtp mode when all smtp fields are populated`() {
        assertTrue(completeSmtpConfig.isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when destination email is blank`() {
        assertFalse(completeSmtpConfig.copy(destinationEmail = "").isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when smtp host is blank`() {
        assertFalse(completeSmtpConfig.copy(smtpHost = "").isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when smtp port is zero`() {
        assertFalse(completeSmtpConfig.copy(smtpPort = 0).isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when smtp port is negative`() {
        assertFalse(completeSmtpConfig.copy(smtpPort = -1).isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when smtp username is blank`() {
        assertFalse(completeSmtpConfig.copy(smtpUsername = "").isComplete)
    }

    @Test
    fun `isComplete returns false for smtp mode when smtp password is blank`() {
        assertFalse(completeSmtpConfig.copy(smtpPassword = "").isComplete)
    }

    @Test
    fun `isComplete returns true for google account mode when destination email and account name are set`() {
        assertTrue(completeGoogleAccountConfig.isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode when destination email is blank`() {
        assertFalse(completeGoogleAccountConfig.copy(destinationEmail = "").isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode when google account name is blank`() {
        assertFalse(completeGoogleAccountConfig.copy(googleAccountName = "").isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode even when smtp fields are populated but account name is blank`() {
        assertFalse(
            completeGoogleAccountConfig.copy(
                smtpHost = "smtp.gmail.com",
                smtpPort = 587,
                smtpUsername = "user@gmail.com",
                smtpPassword = "password",
                googleAccountName = "",
            ).isComplete,
        )
    }

    @Test
    fun `EMPTY has blank destination email and is incomplete`() {
        assertFalse(ForwardingConfig.EMPTY.isComplete)
    }

    @Test
    fun `EMPTY defaults to Google Account auth mode`() {
        assertTrue(ForwardingConfig.EMPTY.authMode == EmailAuthMode.GOOGLE_ACCOUNT)
    }
}
