package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.OAuthTokenProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

fun interface GmailApiCaller {
    fun call(token: String, encodedRfc2822: String): Pair<Int, String>
}

class GmailApiEmailSendRepository(
    private val tokenProvider: OAuthTokenProvider,
    private val apiCaller: GmailApiCaller = GmailApiCaller { token, encodedRfc2822 ->
        val url = URL("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            connection.outputStream.use { it.write("""{"raw":"$encodedRfc2822"}""".toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = if (code != HttpURLConnection.HTTP_OK) {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            } else {
                ""
            }
            Pair(code, body)
        } finally {
            connection.disconnect()
        }
    },
) : EmailSendRepository {

    override fun send(message: SmsMessage, config: ForwardingConfig) {
        val token = tokenProvider.getToken(config.googleAccountName)
        val encoded = encodeMessage(message, config)
        val (code, body) = apiCaller.call(token, encoded)
        if (code == HttpURLConnection.HTTP_OK) return
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
            tokenProvider.invalidateToken(config.googleAccountName, token)
            val freshToken = tokenProvider.getToken(config.googleAccountName)
            val (retryCode, retryBody) = apiCaller.call(freshToken, encoded)
            if (retryCode == HttpURLConnection.HTTP_OK) return
            throw IOException(
                "Gmail API error $retryCode. Open SMS Email Forwarder and re-select your Google Account in Settings. $retryBody".trimEnd(),
            )
        }
        throw IOException("Gmail API error $code. $body".trimEnd())
    }

    private fun encodeMessage(message: SmsMessage, config: ForwardingConfig): String {
        val rfc2822 = "From: ${config.googleAccountName}\r\n" +
            "To: ${config.destinationEmail}\r\n" +
            "Subject: SMS from ${message.sender}\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\n" +
            "From: ${message.sender}\n\n${message.body}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rfc2822.toByteArray(Charsets.UTF_8))
    }
}
