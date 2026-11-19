package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseManifestParserTest {
    private val base = "https://example.test/releases"
    private val parser = ReleaseManifestParser(base)

    private fun entries(count: Int): String = (count downTo 1).joinToString(",", "[", "]") { n ->
        """{"version":"0.1.$n","tag":"v0.1.$n","publishedAt":"2026-01-01T00:00:00Z","notes":"- change $n"}"""
    }

    @Test
    fun `parse returns every manifest entry with its notes and universal apk url`() {
        val releases = parser.parse(entries(15))
        assertEquals(15, releases.size)
        assertEquals(
            AppRelease("0.1.15", "- change 15", "$base/download/v0.1.15/sms-email-forwarder_universal.apk"),
            releases.first(),
        )
        assertEquals("0.1.1", releases.last().version)
    }

    @Test
    fun `parse accepts an empty manifest and a blank notes value`() {
        assertEquals(emptyList<AppRelease>(), parser.parse("[]"))
        assertEquals(
            "",
            parser.parse("""[{"version":"0.1.2","tag":"v0.1.2","publishedAt":"x","notes":""}]""").single().notes,
        )
    }

    @Test
    fun `parse rejects malformed json`() {
        val error = runCatching { parser.parse("{not json") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `parse rejects an entry missing a required field`() {
        val cases = listOf(
            "missing version" to """[{"tag":"v1","notes":"n"}]""",
            "missing notes" to """[{"version":"1.0.0","tag":"v1.0.0"}]""",
            "blank version" to """[{"version":" ","notes":"n"}]""",
            "version not a string" to """[{"version":3,"notes":"n"}]""",
        )
        cases.forEach { (name, json) ->
            val error = runCatching { parser.parse(json) }.exceptionOrNull()
            assertTrue("$name: expected IllegalArgumentException, got $error", error is IllegalArgumentException)
        }
    }
}
