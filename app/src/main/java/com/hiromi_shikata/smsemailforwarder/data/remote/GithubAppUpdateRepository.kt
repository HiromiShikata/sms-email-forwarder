package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
class GithubAppUpdateRepository(
    private val githubRepo: String,
    private val fetchText: (String) -> String = ::httpGetText,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
    private val maxPages: Int = MAX_PAGES,
) : AppUpdateRepository {
    private val parser = GithubReleasesAtomParser(githubRepo)

    override fun fetchReleasesNewerThan(installedVersion: String): List<AppRelease> {
        val collected = mutableListOf<AppRelease>()
        for (page in 1..maxPages) {
            val url = "https://github.com/$githubRepo/releases.atom?page=$page"
            val releases = parser.parse(fetchText(url))
            if (releases.isEmpty()) break
            collected += releases
            if (releases.any { !versionCompare.isNewer(it.version, installedVersion) }) break
        }
        return collected
    }

    companion object {
        const val MAX_PAGES = 10
    }
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
