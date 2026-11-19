package com.hiromi_shikata.smsemailforwarder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PreviousReleaseTagScriptTest {

    private val script = File("../scripts/previous-release-tag.sh").absoluteFile

    private data class Case(
        val name: String,
        val tagsOnStdin: String,
        val currentTag: String,
        val expectedOutput: String,
    )

    private val cases = listOf(
        Case(
            name = "returns the newest tag other than the current tag",
            tagsOnStdin = "v0.1.3\nv0.1.2\nv0.1.1\n",
            currentTag = "v0.1.3",
            expectedOutput = "v0.1.2\n",
        ),
        Case(
            name = "returns the first tag when the current tag is not in the list",
            tagsOnStdin = "v0.1.2\nv0.1.1\n",
            currentTag = "v0.1.3",
            expectedOutput = "v0.1.2\n",
        ),
        Case(
            name = "returns nothing when the tag list is empty",
            tagsOnStdin = "",
            currentTag = "v0.1.3",
            expectedOutput = "",
        ),
        Case(
            name = "returns nothing when the only tag is the current tag",
            tagsOnStdin = "v0.1.3\n",
            currentTag = "v0.1.3",
            expectedOutput = "",
        ),
    )

    @Test
    fun `prints the first listed tag that differs from the current tag, or nothing`() {
        val actual = cases.map { case ->
            case.name to runScript(case.tagsOnStdin, case.currentTag)
        }

        assertEquals(
            cases.map { it.name to it.expectedOutput },
            actual,
        )
    }

    private fun runScript(tagsOnStdin: String, currentTag: String): String {
        val process = ProcessBuilder("bash", script.path, currentTag)
            .redirectErrorStream(false)
            .start()
        process.outputStream.bufferedWriter().use { it.write(tagsOnStdin) }
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        assertEquals("script stderr: $stderr", 0, exitCode)
        return stdout
    }
}
