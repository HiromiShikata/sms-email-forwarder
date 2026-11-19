package com.hiromi_shikata.smsemailforwarder.data.remote

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal fun writeFileAtomically(target: File, writeContent: (File) -> Unit) {
    val directory = target.absoluteFile.parentFile
        ?: throw IOException("Target has no parent directory: ${target.path}")
    val partial = File.createTempFile("${target.name}-", ".part", directory)
    try {
        writeContent(partial)
        Files.move(
            partial.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } finally {
        partial.delete()
    }
}
