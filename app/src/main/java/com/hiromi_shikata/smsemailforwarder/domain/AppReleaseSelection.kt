package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease

fun selectReleasesNewerThan(
    releases: List<AppRelease>,
    installedVersion: String,
    versionCompare: AppVersionCompare = AppVersionCompare(),
): List<AppRelease> = releases
    .filter { versionCompare.isNewer(it.version, installedVersion) }
    .sortedWith { a, b ->
        when {
            versionCompare.isNewer(a.version, b.version) -> -1
            versionCompare.isNewer(b.version, a.version) -> 1
            else -> 0
        }
    }

fun buildUpdateNotificationText(releases: List<AppRelease>): String = releases.joinToString("\n\n") { release ->
    val notes = release.notes.ifBlank { "No change notes." }
    "v${release.version}\n$notes"
}
