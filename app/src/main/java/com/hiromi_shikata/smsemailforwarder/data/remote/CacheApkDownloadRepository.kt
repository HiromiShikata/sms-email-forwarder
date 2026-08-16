package com.hiromi_shikata.smsemailforwarder.data.remote

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File
import java.net.URL

class CacheApkDownloadRepository(
    private val context: Context,
) : ApkDownloadRepository {
    override fun download(url: String): File {
        val file = File(context.cacheDir, "update.apk")
        URL(url).openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
