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
import java.io.InputStream

class VerifiedApkWriteTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val body = "apk-bytes-0123456789".toByteArray()

    @Test
    fun `copies the body exactly and keeps the file when the length matches`() {
        val partFile = tmp.newFile("update.apk.part")

        writeVerifiedApk(partFile, body.size.toLong()) { ByteArrayInputStream(body) }

        assertTrue(partFile.exists())
        assertArrayEquals(body, partFile.readBytes())
    }

    @Test
    fun `throws IOException and deletes the file when the body is shorter than expectedLength`() {
        val partFile = tmp.newFile("update.apk.part")

        assertThrows(IOException::class.java) {
            writeVerifiedApk(partFile, body.size.toLong() + 1) { ByteArrayInputStream(body) }
        }

        assertFalse(partFile.exists())
    }

    @Test
    fun `accepts any body and keeps the file when expectedLength is unknown`() {
        val partFile = tmp.newFile("update.apk.part")

        writeVerifiedApk(partFile, 0L) { ByteArrayInputStream(body) }

        assertTrue(partFile.exists())
        assertArrayEquals(body, partFile.readBytes())
    }

    @Test
    fun `propagates an IOException from open and deletes the file`() {
        val partFile = tmp.newFile("update.apk.part")

        val error = assertThrows(IOException::class.java) {
            writeVerifiedApk(partFile, body.size.toLong()) { throw IOException("open failed") }
        }

        assertEquals("open failed", error.message)
        assertFalse(partFile.exists())
    }

    @Test
    fun `propagates an IOException thrown during read and deletes the file`() {
        val partFile = tmp.newFile("update.apk.part")

        val error = assertThrows(IOException::class.java) {
            writeVerifiedApk(partFile, body.size.toLong()) { FailingAfterBytesInputStream(body, failAfter = 5) }
        }

        assertEquals("read failed", error.message)
        assertFalse(partFile.exists())
    }

    private class FailingAfterBytesInputStream(
        private val bytes: ByteArray,
        private val failAfter: Int,
    ) : InputStream() {
        private var position = 0

        override fun read(): Int {
            if (position >= failAfter) throw IOException("read failed")
            return if (position < bytes.size) bytes[position++].toInt() and 0xFF else -1
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= failAfter) throw IOException("read failed")
            if (position >= bytes.size) return -1
            val count = minOf(length, failAfter - position, bytes.size - position)
            System.arraycopy(bytes, position, buffer, offset, count)
            position += count
            return count
        }
    }
}
