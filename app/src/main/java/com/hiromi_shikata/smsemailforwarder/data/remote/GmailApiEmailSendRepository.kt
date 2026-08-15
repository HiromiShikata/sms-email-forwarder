package com.hiromi_shikata.smsemailforwarder.data.remote

import android.util.Base64
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.OAuthTokenProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GmailApiEmailSendRepository(
    private val tokenProvider: OAuthTokenProvider,
) : EmailSendRepository {

    override fun send(message: SmsMessage, config: ForwardingConfig) {
        val token = tokenProvider.getToken(config.googleAccountName)
        val rfc2822 = buildRfc2822Message(
            from = config.googleAccountName,
            to = config.destinationEmail,
            subject = "SMS from ${message.sender}",
            body = "From: ${message.sender}\n\n${message.body}",
        )
        val encoded = Base64.encodeToString(rfc2822.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            connection.outputStream.use { it.write("""{"raw":"$encoded"}""".toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
                throw IOException("Gmail API error $responseCode: $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRfc2822Message(from: String, to: String, subject: String, body: String): String =
        "From: $from\r\nTo: $to\r\nSubject: $subject\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n$body"
}
