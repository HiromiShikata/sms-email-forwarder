package com.hiromi_shikata.smsemailforwarder.domain.repository

interface OAuthTokenProvider {
    fun getToken(accountName: String): String
}
