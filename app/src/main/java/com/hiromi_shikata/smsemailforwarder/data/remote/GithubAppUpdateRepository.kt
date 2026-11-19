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
) : AppUpdateRepository {
    private val parser = GithubReleasesAtomParser(githubRepo)

    override fun fetchReleasesNewerThan(installedVersion: String): List<AppRelease> {
        val releases = parser.parse(fetchText("https://github.com/$githubRepo/releases.atom"))
        val oldest = releases.lastOrNull()
        if (releases.size >= FEED_ENTRY_LIMIT && oldest != null && versionCompare.isNewer(oldest.version, installedVersion)) {
            throw IllegalStateException(
                "The release feed lists only the newest $FEED_ENTRY_LIMIT releases, and v${oldest.version} is still newer than v$installedVersion",
            )
        }
        return releases
    }

    companion object {
        const val FEED_ENTRY_LIMIT = 10
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
