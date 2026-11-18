package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class SmtpSendFailureClassificationTest {

    @Test
    fun `classifySmtpSendFailure classifies failures as transient or returns the same error`() {
        val cases = listOf(
            ClassificationCase(
                "messaging connect failure",
                MessagingException("Couldn't connect to host", ConnectException()),
                expectTransient = true,
            ),
            ClassificationCase(
                "messaging socket timeout",
                MessagingException("timeout", SocketTimeoutException()),
                expectTransient = true,
            ),
            ClassificationCase(
                "plain io failure without messaging wrapper",
                IOException("socket closed"),
                expectTransient = true,
            ),
            ClassificationCase(
                "authentication failure",
                AuthenticationFailedException("bad password"),
                expectTransient = false,
            ),
            ClassificationCase(
                "runtime configuration failure",
                RuntimeException("incomplete configuration"),
                expectTransient = false,
            ),
        )

        cases.forEach { case ->
            val result = classifySmtpSendFailure(case.error)
            if (case.expectTransient) {
                val transientError = result as? TransientEmailSendException
                assertNotNull("${case.description} must be transient", transientError)
                assertSame("${case.description} must keep the original error as cause", case.error, transientError?.cause)
            } else {
                assertSame("${case.description} must return the same instance", case.error, result)
            }
        }
    }
}

private data class ClassificationCase(
    val description: String,
    val error: Exception,
    val expectTransient: Boolean,
)
