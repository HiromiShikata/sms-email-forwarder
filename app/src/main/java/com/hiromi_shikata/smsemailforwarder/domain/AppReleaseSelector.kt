package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease

object AppReleaseSelector {
    fun newerThan(releases: List<AppRelease>, currentVersion: String): List<AppRelease> {
        val versionCompare = AppVersionCompare()
        return releases
            .filter { versionCompare.isNewer(it.version, currentVersion) }
            .sortedWith { a, b ->
                when {
                    versionCompare.isNewer(a.version, b.version) -> -1
                    versionCompare.isNewer(b.version, a.version) -> 1
                    else -> 0
                }
            }
    }
}
