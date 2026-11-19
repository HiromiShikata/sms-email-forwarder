package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException

class ApkCacheStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val body = "apk-bytes-0123456789".toByteArray()

    @Test
    fun `fresh cache writes the body to update-digest apk and leaves no part file`() {
        val cacheDir = tmp.newFolder("cache")

        val result = cacheApk(cacheDir, "abc", body.size.toLong()) { ByteArrayInputStream(body) }

        assertEquals(java.io.File(cacheDir, "update-abc.apk"), result)
        assertArrayEquals(body, result.readBytes())
        assertFalse(cacheDir.listFiles().orEmpty().any { it.name.endsWith(".part") })
    }

    @Test
    fun `cache hit returns the existing file unchanged without calling open`() {
        val cacheDir = tmp.newFolder("cache")
        val existing = java.io.File(cacheDir, "update-abc.apk").apply { writeText("old") }
        var openCalls = 0

        val result = cacheApk(cacheDir, "abc", body.size.toLong()) {
            openCalls++
            ByteArrayInputStream(body)
        }

        assertEquals(existing, result)
        assertEquals("old", result.readText())
        assertEquals(0, openCalls)
    }

    @Test
    fun `stale finished apks are deleted and non-matching files are kept`() {
        val cacheDir = tmp.newFolder("cache")
        val stale = java.io.File(cacheDir, "update-old.apk").apply { writeText("stale") }
        val other = java.io.File(cacheDir, "other.txt").apply { writeText("keep") }

        cacheApk(cacheDir, "abc", body.size.toLong()) { ByteArrayInputStream(body) }

        assertFalse(stale.exists())
        assertTrue(other.exists())
        assertEquals("keep", other.readText())
    }

    @Test
    fun `open failure propagates and leaves neither the target nor a part file`() {
        val cacheDir = tmp.newFolder("cache")

        assertThrows(IOException::class.java) {
            cacheApk(cacheDir, "abc", body.size.toLong()) { throw IOException("network down") }
        }

        assertFalse(java.io.File(cacheDir, "update-abc.apk").exists())
        assertFalse(cacheDir.listFiles().orEmpty().any { it.name.endsWith(".part") })
    }

    @Test
    fun `length mismatch propagates and leaves neither the target nor a part file`() {
        val cacheDir = tmp.newFolder("cache")

        assertThrows(IOException::class.java) {
            cacheApk(cacheDir, "abc", body.size.toLong() + 1) { ByteArrayInputStream(body) }
        }

        assertFalse(java.io.File(cacheDir, "update-abc.apk").exists())
        assertFalse(cacheDir.listFiles().orEmpty().any { it.name.endsWith(".part") })
    }
}
