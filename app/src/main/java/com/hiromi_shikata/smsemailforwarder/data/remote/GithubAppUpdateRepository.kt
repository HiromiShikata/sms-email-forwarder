package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import java.net.URL

class GithubAppUpdateRepository(
    private val githubRepo: String,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
) : AppUpdateRepository {
    override fun fetchLatest(currentVersion: String): AppUpdate {
        val url = "https://api.github.com/repos/$githubRepo/releases?per_page=30"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        val response = connection.getInputStream().bufferedReader().readText()
        val candidates = GithubReleasesJsonParser.parse(response)
            .filter { !it.draft && !it.prerelease }
            .filter { VERSION_TAG.matches(it.tagName) }
            .mapNotNull { dto ->
                dto.universalApkUrl?.let { apkUrl ->
                    ReleaseCandidate(
                        version = dto.tagName.removePrefix("v"),
                        apkUrl = apkUrl,
                        changeLines = GithubReleasesJsonParser.changeLines(dto.body),
                    )
                }
            }
        val releases = candidates.map { AppRelease(version = it.version, changeLines = it.changeLines) }
        val newest = candidates.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
            ?: return AppUpdate(
                latestVersion = currentVersion,
                downloadUrl = "",
                isUpdateAvailable = false,
                releases = releases,
            )
        return AppUpdate(
            latestVersion = newest.version,
            downloadUrl = newest.apkUrl,
            isUpdateAvailable = versionCompare.isNewer(newest.version, currentVersion),
            releases = releases,
        )
    }

    private fun compareVersions(a: String, b: String): Int = when {
        versionCompare.isNewer(a, b) -> 1
        versionCompare.isNewer(b, a) -> -1
        else -> 0
    }

    private data class ReleaseCandidate(
        val version: String,
        val apkUrl: String,
        val changeLines: List<String>,
    )

    private companion object {
        val VERSION_TAG = Regex("""v?\d+(\.\d+)*""")
    }
}
