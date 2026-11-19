package com.hiromi_shikata.smsemailforwarder.data.remote

import java.io.File
import java.io.IOException
import java.io.InputStream

internal fun writeVerifiedApk(partFile: File, expectedLength: Long, open: () -> InputStream) {
    try {
        val copiedLength = open().use { input ->
            partFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (expectedLength > 0 && copiedLength != expectedLength) {
            throw IOException("Incomplete update download: expected $expectedLength bytes, got $copiedLength")
        }
    } catch (error: Throwable) {
        partFile.delete()
        throw error
    }
}
