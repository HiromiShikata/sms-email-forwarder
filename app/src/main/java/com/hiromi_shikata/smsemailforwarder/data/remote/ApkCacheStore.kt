package com.hiromi_shikata.smsemailforwarder.data.remote

import java.io.File
import java.io.IOException
import java.io.InputStream

private const val CACHE_TAG = "CacheApkDownload"

internal fun cachedApkFile(cacheDir: File, digest: String): File =
    File(cacheDir, "update-$digest.apk")

internal fun cacheApk(cacheDir: File, digest: String, expectedLength: Long, open: () -> InputStream): File =
    cacheApk(cacheDir, digest, expectedLength, open, { from, to -> from.renameTo(to) })

internal fun cacheApk(
    cacheDir: File,
    digest: String,
    expectedLength: Long,
    open: () -> InputStream,
    rename: (File, File) -> Boolean = { from, to -> from.renameTo(to) },
): File {
    val target = cachedApkFile(cacheDir, digest)
    if (target.exists()) return target

    val partFile = File.createTempFile("update-$digest-", ".apk.part", cacheDir)
    var moved = false
    try {
        writeVerifiedApk(partFile, expectedLength, open)
        if (!rename(partFile, target)) {
            throw IOException("Failed to move the downloaded update to ${target.name}")
        }
        moved = true
    } finally {
        if (!moved && partFile.exists() && !partFile.delete()) {
            System.err.println("$CACHE_TAG: Failed to delete partial update file ${partFile.name}")
        }
    }

    staleCachedFiles(cacheDir, target).forEach { stale ->
        if (!stale.delete()) {
            System.err.println("$CACHE_TAG: Failed to delete stale update file ${stale.name}")
        }
    }
    return target
}
