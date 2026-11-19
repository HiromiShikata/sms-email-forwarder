package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateNotificationComposerTest {

    private data class Case(
        val name: String,
        val releases: List<AppRelease>,
        val expectedLines: List<String>,
    )

    private val cases = listOf(
        Case(
            name = "lists each version followed by its change lines, in the given order",
            releases = listOf(
                AppRelease(version = "0.1.350", changeLines = listOf("Add X", "Fix Y")),
                AppRelease(version = "0.1.348", changeLines = listOf("Fix Z")),
            ),
            expectedLines = listOf("v0.1.350", "- Add X", "- Fix Y", "v0.1.348", "- Fix Z"),
        ),
        Case(
            name = "shows a version line alone when a release has no change lines",
            releases = listOf(
                AppRelease(version = "0.1.349", changeLines = emptyList()),
                AppRelease(version = "0.1.348", changeLines = listOf("Fix Z")),
            ),
            expectedLines = listOf("v0.1.349", "v0.1.348", "- Fix Z"),
        ),
    )

    @Test
    fun `bigText lists each release version and its change lines in the given order`() {
        val actual = cases.map { case ->
            case.name to AppUpdateNotificationComposer.bigText(case.releases).lines().filter { it.isNotBlank() }
        }
        assertEquals(cases.map { it.name to it.expectedLines }, actual)
    }

    @Test
    fun `title contains the newest release version`() {
        val releases = listOf(
            AppRelease(version = "0.1.350", changeLines = listOf("Add X")),
            AppRelease(version = "0.1.348", changeLines = listOf("Fix Y")),
        )

        val title = AppUpdateNotificationComposer.title(releases)

        assertTrue("title was: $title", title.contains("0.1.350"))
    }
}
