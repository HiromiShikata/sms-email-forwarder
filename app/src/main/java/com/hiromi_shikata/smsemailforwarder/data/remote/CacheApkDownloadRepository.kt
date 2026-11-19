package com.hiromi_shikata.smsemailforwarder.data.remote

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest

class CacheApkDownloadRepository(
    private val context: Context,
) : ApkDownloadRepository {
    override fun download(url: String): File {
        val file = File(context.cacheDir, "update-${urlDigest(url)}.apk")
        if (file.exists()) return file
        val partFile = File(context.cacheDir, "${file.name}.part")
        URL(url).openStream().use { input ->
            partFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        if (!partFile.renameTo(file)) {
            throw IOException("Failed to move the downloaded update to ${file.name}")
        }
        return file
    }

    private fun urlDigest(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
}
