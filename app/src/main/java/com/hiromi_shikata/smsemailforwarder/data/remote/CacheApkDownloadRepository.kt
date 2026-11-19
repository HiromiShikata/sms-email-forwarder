package com.hiromi_shikata.smsemailforwarder.data.remote

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class CacheApkDownloadRepository(
    private val context: Context,
) : ApkDownloadRepository {
    override fun download(url: String): File {
        val digest = urlDigest(url)
        val cached = cachedApkFile(context.cacheDir, digest)
        if (cached.exists()) return cached
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            return cacheApk(context.cacheDir, digest, connection.contentLengthLong) { connection.inputStream }
        } finally {
            connection.disconnect()
        }
    }

    private fun urlDigest(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }

    private companion object {
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
