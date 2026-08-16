package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository

class ApkDownloadUseCase(
    private val repository: ApkDownloadRepository,
) {
    fun execute(url: String, fileName: String): Long = repository.enqueueDownload(url, fileName)
}
