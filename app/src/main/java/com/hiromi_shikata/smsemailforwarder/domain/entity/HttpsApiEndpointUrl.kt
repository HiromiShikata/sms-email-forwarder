package com.hiromi_shikata.smsemailforwarder.domain.entity

import java.net.URI

fun isHttpsApiEndpointUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}
