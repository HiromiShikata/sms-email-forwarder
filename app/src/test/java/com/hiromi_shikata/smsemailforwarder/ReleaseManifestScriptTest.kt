package com.hiromi_shikata.smsemailforwarder

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseManifestScriptTest {
    private val script = File("../scripts/build-release-notes-manifest.sh").absolutePath

    private data class Run(val exitCode: Int, val stdout: String, val stderr: String)

    private fun run(vararg args: String, generatedAt: String? = null): Run {
        val builder = ProcessBuilder(listOf("bash", script) + args.toList())
        if (generatedAt != null) builder.environment()["MANIFEST_GENERATED_AT"] = generatedAt
        val process = builder.start()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        return Run(process.waitFor(), stdout, stderr)
    }

    private fun fixture(content: String): File = File.createTempFile("releases", ".json").apply {
        writeText(content)
        deleteOnExit()
    }

    @Test
    fun `script writes a newest-first manifest of published releases only`() {
        val input = fixture(
            """
            [
              {"tag_name":"v0.1.2","published_at":"2026-01-02T00:00:00Z","draft":false,"body":"- two"},
              {"tag_name":"v0.1.4","published_at":"2026-01-04T00:00:00Z","draft":true,"body":"- draft"}
            ]
            [
              {"tag_name":"v0.1.3","published_at":"2026-01-03T00:00:00Z","draft":false,"body":"- three"},
              {"tag_name":"v0.1.1","published_at":"2026-01-01T00:00:00Z","draft":false,"body":null}
            ]
            """.trimIndent(),
        )
        val result = run(input.absolutePath)
        assertEquals(result.stderr, 0, result.exitCode)
        val actual = JSONArray(result.stdout)
        val expected = JSONArray(
            """
            [
              {"version":"0.1.3","tag":"v0.1.3","publishedAt":"2026-01-03T00:00:00Z","notes":"- three"},
              {"version":"0.1.2","tag":"v0.1.2","publishedAt":"2026-01-02T00:00:00Z","notes":"- two"},
              {"version":"0.1.1","tag":"v0.1.1","publishedAt":"2026-01-01T00:00:00Z","notes":""}
            ]
            """.trimIndent(),
        )
        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun `script includes the draft being published with the generated time when it has no published date`() {
        val input = fixture(
            """
            [
              {"tag_name":"v0.1.2","published_at":"2026-01-02T00:00:00Z","draft":false,"body":"- two"},
              {"tag_name":"v0.1.3","published_at":null,"draft":true,"body":"- draft three"},
              {"tag_name":"v0.1.4","published_at":null,"draft":true,"body":"- other draft"}
            ]
            """.trimIndent(),
        )
        val result = run(input.absolutePath, "v0.1.3", generatedAt = "2026-01-05T00:00:00Z")
        assertEquals(result.stderr, 0, result.exitCode)
        val expected = JSONArray(
            """
            [
              {"version":"0.1.3","tag":"v0.1.3","publishedAt":"2026-01-05T00:00:00Z","notes":"- draft three"},
              {"version":"0.1.2","tag":"v0.1.2","publishedAt":"2026-01-02T00:00:00Z","notes":"- two"}
            ]
            """.trimIndent(),
        )
        assertEquals(expected.toString(), JSONArray(result.stdout).toString())
    }

    @Test
    fun `script fails with a non-zero exit when the input is not a valid release list`() {
        val cases = listOf(
            "malformed json" to "{not json",
            "object instead of array" to """{"tag_name":"v1"}""",
        )
        cases.forEach { (name, content) ->
            val result = run(fixture(content).absolutePath)
            assertNotEquals("$name must fail", 0, result.exitCode)
            assertTrue("$name must report on stderr", result.stderr.isNotBlank())
            assertEquals("$name must print no manifest", "", result.stdout)
        }
    }

    @Test
    fun `script fails with usage exit code when no input is given or the file is missing`() {
        assertEquals(2, run().exitCode)
        assertEquals(2, run("/nonexistent/releases.json").exitCode)
        assertEquals(2, run(fixture("[]").absolutePath, "v1", "extra").exitCode)
    }

    @Test
    fun `script fails instead of omitting a draft release that the list does not contain`() {
        val input = fixture("""[{"tag_name":"v0.1.2","published_at":"2026-01-02T00:00:00Z","draft":false,"body":"b"}]""")
        val result = run(input.absolutePath, "v0.1.3")
        assertNotEquals(0, result.exitCode)
        assertTrue(result.stderr.contains("v0.1.3"))
        assertEquals("", result.stdout)
    }
}
