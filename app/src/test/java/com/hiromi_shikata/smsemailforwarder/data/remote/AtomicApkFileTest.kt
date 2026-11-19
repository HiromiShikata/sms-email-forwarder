package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class AtomicApkFileTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a successful write replaces the target with the new content and leaves no partial file`() {
        val target = folder.newFile("update.apk").apply { writeText("old") }
        writeFileAtomically(target) { partial -> partial.writeText("new") }
        assertEquals("new", target.readText())
        assertEquals(listOf("update.apk"), folder.root.list()!!.toList())
    }

    @Test
    fun `a failed write leaves the previous target untouched and removes its partial file`() {
        val target = folder.newFile("update.apk").apply { writeText("old") }
        val error = runCatching {
            writeFileAtomically(target) { partial ->
                partial.writeText("half")
                throw IOException("connection reset")
            }
        }.exceptionOrNull()
        assertTrue(error is IOException)
        assertEquals("old", target.readText())
        assertEquals(listOf("update.apk"), folder.root.list()!!.toList())
    }

    @Test
    fun `concurrent writers each use their own partial file`() {
        val target = folder.newFile("update.apk")
        val seen = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val threads = (1..4).map { index ->
            Thread {
                writeFileAtomically(target) { partial ->
                    seen += partial.name
                    partial.writeText("writer $index")
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(4, seen.size)
        assertTrue(target.readText().startsWith("writer "))
    }
}
