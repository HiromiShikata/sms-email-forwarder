package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.HttpsApiUnauthorizedException
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class HttpsApiEmailSendRepository(
    private val openConnection: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : EmailSendRepository {
    override fun send(message: SmsMessage, config: ForwardingConfig) {
        val response = try {
            postThenFollowRedirects(URL(config.apiEndpointUrl), buildHttpsApiRequestBody(message, config.apiKey))
        } catch (error: IOException) {
            throw TransientEmailSendException(
                "HTTPS API request failed (${error.javaClass.simpleName}).",
                error,
            )
        }
        interpretHttpsApiResponse(response.statusCode, response.body)
    }

    private fun postThenFollowRedirects(endpoint: URL, requestBody: String): HttpsApiHttpResponse {
        var response = request(endpoint, method = "POST", body = requestBody)
        var redirectCount = 0
        while (response.statusCode in REDIRECT_STATUS_CODES) {
            val location = response.location
                ?: throw TransientEmailSendException("HTTPS API redirect had no Location header.", null)
            redirectCount += 1
            if (redirectCount > MAX_REDIRECTS) {
                throw TransientEmailSendException("HTTPS API exceeded $MAX_REDIRECTS redirects.", null)
            }
            response = request(URL(endpoint, location), method = "GET", body = null)
        }
        return response
    }

    private fun request(url: URL, method: String, body: String?): HttpsApiHttpResponse {
        val connection = openConnection(url)
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = HTTPS_API_TIMEOUT_MILLIS
            connection.readTimeout = HTTPS_API_TIMEOUT_MILLIS
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode >= HTTP_BAD_REQUEST) connection.errorStream else connection.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpsApiHttpResponse(
                statusCode = statusCode,
                location = connection.getHeaderField("Location"),
                body = text,
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        const val MAX_REDIRECTS = 5
        const val HTTP_BAD_REQUEST = 400
    }
}

internal data class HttpsApiHttpResponse(
    val statusCode: Int,
    val location: String?,
    val body: String,
)

internal const val HTTPS_API_TIMEOUT_MILLIS = 30_000

private val HTTP_SUCCESS_RANGE = 200..299
private val RECEIVED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

internal fun buildHttpsApiRequestBody(message: SmsMessage, apiKey: String): String =
    JSONObject()
        .put("apiKey", apiKey)
        .put("sender", message.sender)
        .put("body", message.body)
        .put("receivedAt", RECEIVED_AT_FORMAT.format(Instant.ofEpochMilli(message.timestamp)))
        .toString()

internal fun interpretHttpsApiResponse(statusCode: Int, body: String) {
    if (statusCode !in HTTP_SUCCESS_RANGE) {
        throw TransientEmailSendException("HTTPS API returned HTTP $statusCode.", null)
    }
    val json = try {
        JSONObject(body)
    } catch (error: JSONException) {
        throw TransientEmailSendException("HTTPS API returned a non-JSON response.", error)
    }
    when (json.optString("result")) {
        "ok" -> return
        "ng" -> if (json.optString("message") == "unauthorized") {
            throw HttpsApiUnauthorizedException()
        } else {
            throw TransientEmailSendException("HTTPS API reported a temporary failure.", null)
        }
        else -> throw TransientEmailSendException("HTTPS API returned an unexpected result.", null)
    }
}
