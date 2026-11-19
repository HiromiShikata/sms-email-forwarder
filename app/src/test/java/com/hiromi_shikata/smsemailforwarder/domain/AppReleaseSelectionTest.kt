package com.hiromi_shikata.smsemailforwarder.domain

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Test

class AppReleaseSelectionTest {
    private fun release(version: String) = AppRelease(version, "notes $version", "url-$version")

    @Test
    fun `selectReleasesNewerThan keeps every newer release newest first`() {
        val cases = listOf(
            "several newer releases are all kept, newest first" to Triple(
                listOf("0.1.343", "0.1.346", "0.1.345", "0.1.344"),
                "0.1.344",
                listOf("0.1.346", "0.1.345"),
            ),
            "same version gives nothing" to Triple(listOf("0.1.345"), "0.1.345", emptyList()),
            "older releases give nothing" to Triple(listOf("0.1.300"), "0.1.345", emptyList()),
            "major version beats minor" to Triple(listOf("1.0.0", "0.9.9"), "0.9.0", listOf("1.0.0", "0.9.9")),
        )
        cases.forEach { (name, case) ->
            val (versions, installed, expected) = case
            val actual = selectReleasesNewerThan(versions.map(::release), installed).map { it.version }
            assertEquals(name, expected, actual)
        }
    }

    @Test
    fun `buildUpdateNotificationText lists every release notes in order`() {
        val text = buildUpdateNotificationText(
            listOf(
                AppRelease("0.1.346", "- B", "u1"),
                AppRelease("0.1.345", "", "u2"),
            ),
        )
        assertEquals("v0.1.346\n- B\n\nv0.1.345\nNo change notes.", text)
    }
}
