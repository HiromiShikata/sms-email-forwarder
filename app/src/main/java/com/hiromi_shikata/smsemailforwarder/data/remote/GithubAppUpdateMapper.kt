package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate

object GithubAppUpdateMapper {
    fun toAppUpdate(
        dtos: List<GithubReleaseDto>,
        currentVersion: String,
        versionCompare: AppVersionCompare,
    ): AppUpdate {
        val validReleases = dtos
            .filter { !it.draft && !it.prerelease }
            .filter { VERSION_TAG.matches(it.tagName) }
            .sortedWith { a, b ->
                val aVersion = a.tagName.removePrefix("v")
                val bVersion = b.tagName.removePrefix("v")
                when {
                    versionCompare.isNewer(aVersion, bVersion) -> -1
                    versionCompare.isNewer(bVersion, aVersion) -> 1
                    else -> 0
                }
            }
        val releases = validReleases.map {
            AppRelease(
                version = it.tagName.removePrefix("v"),
                changeLines = GithubReleasesJsonParser.changeLines(it.body),
            )
        }
        val newest = validReleases.firstOrNull { it.universalApkUrl != null }
        val apkUrl = newest?.universalApkUrl
            ?: return AppUpdate(
                latestVersion = currentVersion,
                downloadUrl = "",
                isUpdateAvailable = false,
                releases = releases,
            )
        val latestVersion = newest.tagName.removePrefix("v")
        return AppUpdate(
            latestVersion = latestVersion,
            downloadUrl = apkUrl,
            isUpdateAvailable = versionCompare.isNewer(latestVersion, currentVersion),
            releases = releases,
        )
    }

    private val VERSION_TAG = Regex("""v?\d+(\.\d+)*""")
}
