package com.hiromi_shikata.smsemailforwarder.data.remote

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class CacheApkDownloadRepository(
    private val context: Context,
) : ApkDownloadRepository {
    override fun download(url: String): File {
        val digest = urlDigest(url)
        val file = File(context.cacheDir, "update-$digest.apk")
        if (file.exists()) return file
        val partFile = File.createTempFile("update-$digest-", ".apk.part", context.cacheDir)
        var renamed = false
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                writeVerifiedApk(partFile, connection.contentLengthLong) { connection.inputStream }
            } finally {
                connection.disconnect()
            }
            if (!partFile.renameTo(file)) {
                throw IOException("Failed to move the downloaded update to ${file.name}")
            }
            renamed = true
        } finally {
            if (!renamed && partFile.exists() && !partFile.delete()) {
                System.err.println("$TAG: Failed to delete partial update file ${partFile.name}")
            }
        }
        staleCachedFiles(context.cacheDir, file).forEach { stale ->
            if (!stale.delete()) {
                System.err.println("$TAG: Failed to delete stale update file ${stale.name}")
            }
        }
        return file
    }

    private fun urlDigest(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "CacheApkDownload"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
    }
}

internal fun staleCachedFiles(cacheDir: File, keep: File): List<File> =
    cacheDir.listFiles().orEmpty().filter { candidate ->
        candidate.isFile &&
            candidate != keep &&
            candidate.name.startsWith("update-") &&
            candidate.name.endsWith(".apk")
    }
