package com.hiromi_shikata.smsemailforwarder.data.remote

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CacheApkDownloadRepository(
    private val context: Context,
) : ApkDownloadRepository {
    override fun download(url: String): File {
        val target = File(context.cacheDir, "update.apk")
        val partial = File(context.cacheDir, "update.apk.part")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code while downloading $url")
            }
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            throw IOException("Could not move the downloaded APK into place")
        }
        return target
    }
}
