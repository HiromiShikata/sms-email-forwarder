package com.hiromi_shikata.smsemailforwarder

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ReleaseBodyScriptTest {

    private val script = File("../scripts/compose-release-body.sh").absoluteFile
    private lateinit var repo: File
    private lateinit var outputFile: File
    private val shortShaBySubject = mutableMapOf<String, String>()

    private val boilerplate =
        "Automated build of main at abc1234 (version 0.1.2, build 42). " +
            "Install sms-email-forwarder_arm64-v8a.apk for most modern Android devices, " +
            "or sms-email-forwarder_universal.apk for any ABI."

    @Before
    fun createRepository() {
        repo = Files.createTempDirectory("release-body-test").toFile()
        outputFile = File(repo.parentFile, "${repo.name}-release-body.md")
        git("init", "-q")
        git("symbolic-ref", "HEAD", "refs/heads/main")
        git("config", "user.name", "Test Author")
        git("config", "user.email", "test@example.com")
        git("config", "commit.gpgsign", "false")
    }

    @After
    fun deleteRepository() {
        repo.deleteRecursively()
        outputFile.delete()
    }

    @Test
    fun `lists commits after the previous tag under the previous tag heading`() {
        commit("feat: base", day = 1)
        git("tag", "v0.1.1")
        commit("feat: alpha", day = 2)
        commit("fix: beta", day = 3)

        val exitCode = runScript("0.1.2", "42", "abc1234", "v0.1.1")

        assertEquals(0, exitCode)
        assertEquals(
            listOf(
                boilerplate,
                "",
                "## Changes since v0.1.1",
                "",
                line("fix: beta"),
                line("feat: alpha"),
            ),
            outputFile.readLines(),
        )
    }

    @Test
    fun `uses the start of the history heading when the previous tag is empty`() {
        commit("feat: one", day = 1)
        commit("fix: two", day = 2)

        val exitCode = runScript("0.1.2", "42", "abc1234", "")

        assertEquals(0, exitCode)
        assertEquals(
            listOf(
                boilerplate,
                "",
                "## Changes since the start of the history",
                "",
                line("fix: two"),
                line("feat: one"),
            ),
            outputFile.readLines(),
        )
    }

    @Test
    fun `states no changes when there are no commits after the previous tag`() {
        commit("feat: base", day = 1)
        git("tag", "v0.1.1")

        val exitCode = runScript("0.1.2", "42", "abc1234", "v0.1.1")

        assertEquals(0, exitCode)
        assertEquals(
            listOf(
                boilerplate,
                "",
                "## Changes since v0.1.1",
                "",
                "No changes since the previous release.",
            ),
            outputFile.readLines(),
        )
    }

    private fun line(subject: String): String = "- $subject (${shortShaBySubject.getValue(subject)})"

    private fun commit(subject: String, day: Int) {
        val file = File(repo, "history.txt")
        file.appendText("$subject\n")
        git("add", "history.txt", day = day)
        git("commit", "-q", "-m", subject, day = day)
        shortShaBySubject[subject] = git("rev-parse", "--short", "HEAD").trim()
    }

    private fun runScript(vararg args: String): Int {
        val process = ProcessBuilder(listOf("bash", script.path) + args + outputFile.path)
            .directory(repo)
            .start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        assertEquals("script stdout: $stdout stderr: $stderr", 0, exitCode)
        return exitCode
    }

    private fun git(vararg args: String, day: Int? = null): String {
        val builder = ProcessBuilder(listOf("git") + args)
            .directory(repo)
            .redirectErrorStream(true)
        if (day != null) {
            val date = "2020-01-%02dT00:00:00Z".format(day)
            builder.environment()["GIT_AUTHOR_DATE"] = date
            builder.environment()["GIT_COMMITTER_DATE"] = date
        }
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        assertEquals("git ${args.joinToString(" ")} failed: $output", 0, exitCode)
        return output
    }
}
