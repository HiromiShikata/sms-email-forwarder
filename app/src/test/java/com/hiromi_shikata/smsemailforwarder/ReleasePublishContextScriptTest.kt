package com.hiromi_shikata.smsemailforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

class ReleasePublishContextScriptTest {
    private val script = File("../scripts/release-publish-context.sh").absolutePath

    private val releases = """
        [
          {"tag_name":"v0.1.5","published_at":"2026-01-05T00:00:00Z","draft":false},
          {"tag_name":"v0.1.3","published_at":"2026-01-03T00:00:00Z","draft":false},
          {"tag_name":"v0.1.2","published_at":"2026-01-02T00:00:00Z","draft":false},
          {"tag_name":"v0.1.9","published_at":null,"draft":true}
        ]
    """.trimIndent()

    private fun run(vararg args: String): Triple<Int, String, String> {
        val process = ProcessBuilder(listOf("bash", script) + args.toList()).start()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        return Triple(process.waitFor(), stdout, stderr)
    }

    private fun fixture(content: String): File = File.createTempFile("releases", ".json").apply {
        writeText(content)
        deleteOnExit()
    }

    @Test
    fun `an older commit publishing after a newer release gets the newest lower release as previous and is not latest`() {
        val (code, out, err) = run(fixture(releases).absolutePath, "v0.1.4")
        assertEquals(err, 0, code)
        assertEquals("PREVIOUS_TAG=v0.1.3\nIS_NEWEST=false\n", out)
    }

    @Test
    fun `the newest release gets the newest published release as previous and is latest`() {
        val (code, out, _) = run(fixture(releases).absolutePath, "v0.1.6")
        assertEquals(0, code)
        assertEquals("PREVIOUS_TAG=v0.1.5\nIS_NEWEST=true\n", out)
    }

    @Test
    fun `the first release has no previous tag and drafts are ignored`() {
        val (code, out, _) = run(fixture("[]").absolutePath, "v0.1.1")
        assertEquals(0, code)
        assertEquals("PREVIOUS_TAG=\nIS_NEWEST=true\n", out)
    }

    @Test
    fun `a re-run of an already published tag ignores its own entry`() {
        val (code, out, _) = run(fixture(releases).absolutePath, "v0.1.5")
        assertEquals(0, code)
        assertEquals("PREVIOUS_TAG=v0.1.3\nIS_NEWEST=true\n", out)
    }

    @Test
    fun `versions compare numerically not lexically`() {
        val input = fixture("""[{"tag_name":"v0.1.10","published_at":"x","draft":false}]""")
        val (code, out, _) = run(input.absolutePath, "v0.1.9")
        assertEquals(0, code)
        assertEquals("PREVIOUS_TAG=\nIS_NEWEST=false\n", out)
    }

    @Test
    fun `usage and malformed input fail with a non-zero exit`() {
        assertEquals(2, run().first)
        val bad = run(fixture("{").absolutePath, "v1")
        assertNotEquals(0, bad.first)
        assertEquals("", bad.second)
    }
}
