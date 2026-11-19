package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StaleApkCleanupTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private data class Case(
        val name: String,
        val files: List<String>,
        val directories: List<String>,
        val keep: String,
        val expectedStaleNames: List<String>,
    )

    private val cases = listOf(
        Case(
            name = "returns other finished update APKs, but not the kept file",
            files = listOf("update-aaaa.apk", "update-bbbb.apk.part", "update-cccc.apk"),
            directories = emptyList(),
            keep = "update-cccc.apk",
            expectedStaleNames = listOf("update-aaaa.apk"),
        ),
        Case(
            name = "leaves an in-flight partial download alone",
            files = listOf("update-xyz.apk.part"),
            directories = emptyList(),
            keep = "update-keep.apk",
            expectedStaleNames = emptyList(),
        ),
        Case(
            name = "ignores files that do not match the update APK naming and subdirectories",
            files = listOf(
                "other.txt",
                "update-dddd.apk",
                "update-eeee.apk.bak",
                "cache.apk",
                "update-ffff.txt",
            ),
            directories = listOf("update-gggg.apk"),
            keep = "update-dddd.apk",
            expectedStaleNames = emptyList(),
        ),
        Case(
            name = "returns an empty list when only the kept file exists",
            files = listOf("update-hhhh.apk"),
            directories = emptyList(),
            keep = "update-hhhh.apk",
            expectedStaleNames = emptyList(),
        ),
    )

    @Test
    fun `staleCachedFiles returns the update files other than the kept one`() {
        val actual = cases.map { case ->
            val dir = tmp.newFolder(case.name.hashCode().toString())
            case.files.forEach { File(dir, it).writeText("x") }
            case.directories.forEach { File(dir, it).mkdirs() }
            val keep = File(dir, case.keep)
            val stale = staleCachedFiles(cacheDir = dir, keep = keep)
            case.name to stale.map { it.name }.sorted()
        }
        assertEquals(cases.map { it.name to it.expectedStaleNames }, actual)
    }
}
