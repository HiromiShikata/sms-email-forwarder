package com.hiromi_shikata.smsemailforwarder.domain.repository

interface ApkDownloadRepository {
    fun enqueueDownload(url: String, fileName: String): Long
}
