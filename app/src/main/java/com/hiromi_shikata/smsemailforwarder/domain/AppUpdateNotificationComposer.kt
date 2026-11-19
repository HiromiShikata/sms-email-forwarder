package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease

object AppUpdateNotificationComposer {
    fun title(releases: List<AppRelease>): String {
        val newestVersion = releases.firstOrNull()?.version.orEmpty()
        return "Update $newestVersion is ready to install"
    }

    fun bigText(releases: List<AppRelease>): String =
        releases.joinToString(separator = "\n") { release ->
            (listOf("v${release.version}") + release.changeLines.map { "- $it" }).joinToString(separator = "\n")
        }
}
