package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Test

class AppReleaseSelectorTest {

    private data class Case(
        val name: String,
        val releases: List<AppRelease>,
        val currentVersion: String,
        val expected: List<AppRelease>,
    )

    private val release0350 = AppRelease(version = "0.1.350", changeLines = listOf("Add X"))
    private val release0348 = AppRelease(version = "0.1.348", changeLines = listOf("Fix Y"))
    private val release0340 = AppRelease(version = "0.1.340", changeLines = listOf("Old change"))

    private val cases = listOf(
        Case(
            name = "keeps only releases newer than the installed version, newest first",
            releases = listOf(release0340, release0350, release0348),
            currentVersion = "0.1.345",
            expected = listOf(release0350, release0348),
        ),
        Case(
            name = "compares numerically so 0.1.10 is newer than 0.1.9",
            releases = listOf(
                AppRelease(version = "0.1.9", changeLines = listOf("Nine")),
                AppRelease(version = "0.1.10", changeLines = listOf("Ten")),
            ),
            currentVersion = "0.1.8",
            expected = listOf(
                AppRelease(version = "0.1.10", changeLines = listOf("Ten")),
                AppRelease(version = "0.1.9", changeLines = listOf("Nine")),
            ),
        ),
        Case(
            name = "returns nothing when the installed version is the newest",
            releases = listOf(release0350, release0348),
            currentVersion = "0.1.350",
            expected = emptyList(),
        ),
        Case(
            name = "returns nothing for an empty release list",
            releases = emptyList(),
            currentVersion = "0.1.0",
            expected = emptyList(),
        ),
        Case(
            name = "returns every release when the installed version is older than all of them",
            releases = listOf(release0348, release0350),
            currentVersion = "0.1.0",
            expected = listOf(release0350, release0348),
        ),
    )

    @Test
    fun `newerThan returns the releases newer than the installed version, newest first`() {
        val actual = cases.map { it.name to AppReleaseSelector.newerThan(it.releases, it.currentVersion) }
        assertEquals(cases.map { it.name to it.expected }, actual)
    }
}
