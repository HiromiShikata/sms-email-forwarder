package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ReleaseManifestAppUpdateRepository(
    private val releasesBaseUrl: String,
    private val fetchText: (String) -> String = ::httpGetText,
) : AppUpdateRepository {
    private val parser = ReleaseManifestParser(releasesBaseUrl)

    override fun fetchReleases(): List<AppRelease> =
        parser.parse(fetchText("$releasesBaseUrl/latest/download/release-notes.json"))
}

internal fun httpGetText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 15_000
    connection.setRequestProperty("User-Agent", "sms-email-forwarder-app")
    try {
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP $code while reading $url")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
