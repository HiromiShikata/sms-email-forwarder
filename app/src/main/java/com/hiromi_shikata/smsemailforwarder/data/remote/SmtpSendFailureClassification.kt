package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import java.io.IOException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

internal fun classifySmtpSendFailure(error: Exception): Exception {
    val causes = generateSequence<Throwable>(error) { current -> current.cause?.takeIf { it !== current } }.toList()
    if (causes.any { it is AuthenticationFailedException }) return error
    val isTransient = causes.any { it is IOException || it is MessagingException }
    return if (isTransient) TransientEmailSendException(error.message, error) else error
}
