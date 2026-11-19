package com.hiromi_shikata.smsemailforwarder

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ReleaseNotesScriptTest {

    private val script = File("../scripts/generate-release-notes.sh").absoluteFile
    private lateinit var repo: File
    private val shortShaBySubject = mutableMapOf<String, String>()

    @Before
    fun createRepository() {
        repo = Files.createTempDirectory("release-notes-test").toFile()
        git("init", "-q")
        git("symbolic-ref", "HEAD", "refs/heads/main")
        git("config", "user.name", "Test Author")
        git("config", "user.email", "test@example.com")
        git("config", "commit.gpgsign", "false")

        commit("feat: first", day = 1)
        commit("fix: second", day = 2)
        git("tag", "v0.1.0")
        commit("feat: third", day = 3)
        git("checkout", "-q", "-b", "side")
        commit("fix: side work", day = 4)
        git("checkout", "-q", "main")
        commit("feat: fifth", day = 5)
        git("merge", "--no-ff", "-q", "-m", "Merge branch 'side'", "side", day = 6)
    }

    @After
    fun deleteRepository() {
        repo.deleteRecursively()
    }

    @Test
    fun `lists non-merge commits after the previous tag newest first, excluding the tag commit and merges`() {
        val output = runScript("v0.1.0")

        assertEquals(
            listOf(
                line("feat: fifth"),
                line("fix: side work"),
                line("feat: third"),
            ),
            output.lines().filter { it.isNotBlank() },
        )
    }

    @Test
    fun `lists the newest non-merge commits when the previous tag is empty or does not resolve`() {
        val previousTags = listOf("", "v9.9.9")

        val actual = previousTags.map { previousTag ->
            previousTag to runScript(previousTag).lines().filter { it.isNotBlank() }
        }

        val allNonMergeCommits = listOf(
            line("feat: fifth"),
            line("fix: side work"),
            line("feat: third"),
            line("fix: second"),
            line("feat: first"),
        )
        assertEquals(previousTags.map { it to allNonMergeCommits }, actual)
    }

    @Test
    fun `lists commits up to the given head ref`() {
        val output = runScript("v0.1.0", "side")

        assertEquals(
            listOf(
                line("fix: side work"),
                line("feat: third"),
            ),
            output.lines().filter { it.isNotBlank() },
        )
    }

    private fun line(subject: String): String = "- $subject (${shortShaBySubject.getValue(subject)})"

    private fun commit(subject: String, day: Int) {
        File(repo, "history.txt").appendText("$subject\n")
        git("add", "history.txt", day = day)
        git("commit", "-q", "-m", subject, day = day)
        shortShaBySubject[subject] = git("rev-parse", "--short", "HEAD").trim()
    }

    private fun runScript(vararg args: String): String {
        val process = ProcessBuilder(listOf("bash", script.path) + args)
            .directory(repo)
            .start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        assertEquals("script stderr: $stderr", 0, exitCode)
        return stdout
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
