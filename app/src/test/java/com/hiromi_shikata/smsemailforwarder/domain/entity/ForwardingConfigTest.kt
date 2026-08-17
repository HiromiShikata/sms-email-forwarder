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
    )

    private val completeGoogleAccountConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "app-password",
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
    fun `isComplete returns true for google account mode when destination email and smtp credentials are set`() {
        assertTrue(completeGoogleAccountConfig.isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode when destination email is blank`() {
        assertFalse(completeGoogleAccountConfig.copy(destinationEmail = "").isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode when gmail address is blank`() {
        assertFalse(completeGoogleAccountConfig.copy(smtpUsername = "").isComplete)
    }

    @Test
    fun `isComplete returns false for google account mode when app password is blank`() {
        assertFalse(completeGoogleAccountConfig.copy(smtpPassword = "").isComplete)
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
