package com.hiromi_shikata.smsemailforwarder.domain.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingConfigTest {
    private val completeConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "app-password",
    )

    @Test
    fun `isComplete returns true when all fields are populated`() {
        assertTrue(completeConfig.isComplete)
    }

    @Test
    fun `isComplete returns false when destination email is blank`() {
        assertFalse(completeConfig.copy(destinationEmail = "").isComplete)
    }

    @Test
    fun `isComplete returns false when smtp host is blank`() {
        assertFalse(completeConfig.copy(smtpHost = "").isComplete)
    }

    @Test
    fun `isComplete returns false when smtp port is zero`() {
        assertFalse(completeConfig.copy(smtpPort = 0).isComplete)
    }

    @Test
    fun `isComplete returns false when smtp port is negative`() {
        assertFalse(completeConfig.copy(smtpPort = -1).isComplete)
    }

    @Test
    fun `isComplete returns false when smtp username is blank`() {
        assertFalse(completeConfig.copy(smtpUsername = "").isComplete)
    }

    @Test
    fun `isComplete returns false when smtp password is blank`() {
        assertFalse(completeConfig.copy(smtpPassword = "").isComplete)
    }

    @Test
    fun `EMPTY has blank destination email`() {
        assertFalse(ForwardingConfig.EMPTY.isComplete)
    }
}
