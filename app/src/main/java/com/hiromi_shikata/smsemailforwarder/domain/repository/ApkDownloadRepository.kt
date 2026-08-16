package com.hiromi_shikata.smsemailforwarder.domain.repository

import java.io.File

interface ApkDownloadRepository {
    fun download(url: String): File
}
