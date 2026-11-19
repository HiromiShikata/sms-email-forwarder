package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.HttpsApiUnauthorizedException
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Instant

class HttpsApiEmailSendRepositoryTest {
    private val sms = SmsMessage(
        sender = "+81 90-1234-5678",
        body = "本文 \"quoted\"\nline2",
        timestamp = Instant.parse("2026-11-18T20:11:03Z").toEpochMilli(),
    )

    @Test
    fun `request body carries apiKey sender body and receivedAt in UTC ISO-8601`() {
        val cases = listOf(
            BodyCase(
                label = "plain ASCII",
                message = SmsMessage(sender = "A", body = "B", timestamp = Instant.parse("2026-11-18T20:11:03Z").toEpochMilli()),
                apiKey = "key-1",
                expectedReceivedAt = "2026-11-18T20:11:03Z",
            ),
            BodyCase(
                label = "non-ASCII, quotes and newline with millisecond timestamp",
                message = sms.copy(timestamp = Instant.parse("2026-01-02T03:04:05.678Z").toEpochMilli()),
                apiKey = "秘密-2",
                expectedReceivedAt = "2026-01-02T03:04:05Z",
            ),
        )
        for (case in cases) {
            val json = JSONObject(buildHttpsApiRequestBody(case.message, case.apiKey))

            assertEquals(case.label, case.apiKey, json.getString("apiKey"))
            assertEquals(case.label, case.message.sender, json.getString("sender"))
            assertEquals(case.label, case.message.body, json.getString("body"))
            assertEquals(case.label, case.expectedReceivedAt, json.getString("receivedAt"))
        }
    }

    @Test
    fun `response mapping returns success for ok and classifies every other outcome`() {
        val cases = listOf(
            ResponseCase("ok", 200, """{"result":"ok"}""", expectedClass = null),
            ResponseCase("unauthorized", 200, """{"result":"ng","message":"unauthorized"}""", HttpsApiUnauthorizedException::class.java),
            ResponseCase("other ng is transient", 200, """{"result":"ng","message":"quota exceeded"}""", TransientEmailSendException::class.java),
            ResponseCase("unknown result is transient", 200, """{"result":"maybe"}""", TransientEmailSendException::class.java),
            ResponseCase("non-JSON body is transient", 200, "<html>login</html>", TransientEmailSendException::class.java),
            ResponseCase("JSON array is transient", 200, "[]", TransientEmailSendException::class.java),
            ResponseCase("HTTP 500 is transient even with ok body", 500, """{"result":"ok"}""", TransientEmailSendException::class.java),
            ResponseCase("HTTP 503 is transient", 503, "", TransientEmailSendException::class.java),
            ResponseCase("HTTP 201 with ok body is success", 201, """{"result":"ok"}""", expectedClass = null),
        )
        for (case in cases) {
            val thrown = captureThrowable { interpretHttpsApiResponse(case.statusCode, case.body) }
            if (case.expectedClass == null) {
                assertNull(case.label, thrown)
            } else {
                assertEquals(case.label, case.expectedClass, thrown?.javaClass)
            }
        }
    }

    @Test
    fun `send posts JSON once and follows a redirect with GET without re-posting`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 302, location = "/result") }
            server.route("/result") { Reply(status = 200, body = """{"result":"ok"}""") }

            HttpsApiEmailSendRepository().send(sms, configFor("${server.baseUrl}/post"))

            assertEquals(
                listOf("POST /post", "GET /result"),
                server.requests.map { "${it.method} ${it.path}" },
            )
            val post = server.requests.first()
            assertEquals("application/json", post.contentType)
            assertEquals("key-1", JSONObject(post.body).getString("apiKey"))
        }
    }

    @Test
    fun `send maps an unauthorized result to a non-transient failure without retrying`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 200, body = """{"result":"ng","message":"unauthorized"}""") }

            val thrown = captureThrowable { HttpsApiEmailSendRepository().send(sms, configFor("${server.baseUrl}/post")) }

            assertTrue(thrown is HttpsApiUnauthorizedException)
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun `send maps HTTP 500 to a transient failure`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 500, body = "error") }

            val thrown = captureThrowable { HttpsApiEmailSendRepository().send(sms, configFor("${server.baseUrl}/post")) }

            assertTrue(thrown is TransientEmailSendException)
        }
    }

    @Test
    fun `send maps a redirect loop to a transient failure`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 302, location = "/loop") }
            server.route("/loop") { Reply(status = 302, location = "/loop") }

            val thrown = captureThrowable { HttpsApiEmailSendRepository().send(sms, configFor("${server.baseUrl}/post")) }

            assertTrue(thrown is TransientEmailSendException)
        }
    }

    @Test
    fun `send maps a redirect without a Location header to a transient failure`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 302) }

            val thrown = captureThrowable { HttpsApiEmailSendRepository().send(sms, configFor("${server.baseUrl}/post")) }

            assertTrue(thrown is TransientEmailSendException)
            assertEquals(1, server.requests.size)
        }
    }

    @Test
    fun `send maps a refused connection to a transient failure`() {
        val port = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }

        val thrown = captureThrowable {
            HttpsApiEmailSendRepository().send(sms, configFor("http://127.0.0.1:$port/post"))
        }

        assertTrue(thrown is TransientEmailSendException)
    }

    @Test
    fun `connections use 30 second connect and read timeouts`() {
        FakeHttpsApiServer().use { server ->
            server.route("/post") { Reply(status = 200, body = """{"result":"ok"}""") }
            val connections = mutableListOf<HttpURLConnection>()
            val repository = HttpsApiEmailSendRepository { url ->
                (url.openConnection() as HttpURLConnection).also { connections += it }
            }

            repository.send(sms, configFor("${server.baseUrl}/post"))

            assertEquals(1, connections.size)
            assertEquals(30_000, connections.single().connectTimeout)
            assertEquals(30_000, connections.single().readTimeout)
        }
    }

    private fun configFor(endpoint: String) = ForwardingConfig(
        destinationEmail = "",
        authMode = EmailAuthMode.HTTPS_API,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "",
        smtpPassword = "",
        apiEndpointUrl = endpoint,
        apiKey = "key-1",
    )

    private fun captureThrowable(block: () -> Unit): Throwable? =
        try {
            block()
            null
        } catch (error: Throwable) {
            error
        }
}

private data class BodyCase(
    val label: String,
    val message: SmsMessage,
    val apiKey: String,
    val expectedReceivedAt: String,
)

private data class ResponseCase(
    val label: String,
    val statusCode: Int,
    val body: String,
    val expectedClass: Class<out Throwable>? = null,
)

private data class RecordedRequest(val method: String, val path: String, val contentType: String?, val body: String)

private data class Reply(val status: Int, val body: String = "", val location: String? = null)

private class FakeHttpsApiServer : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    val requests = mutableListOf<RecordedRequest>()
    val baseUrl: String = "http://127.0.0.1:${server.address.port}"

    init {
        server.start()
    }

    fun route(path: String, reply: (RecordedRequest) -> Reply) {
        server.createContext(path) { exchange: HttpExchange ->
            val request = RecordedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                contentType = exchange.requestHeaders.getFirst("Content-Type"),
                body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() },
            )
            requests += request
            val response = reply(request)
            response.location?.let { exchange.responseHeaders.add("Location", it) }
            val bytes = response.body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(response.status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
    }

    override fun close() {
        server.stop(0)
    }
}
