package com.hiromi_shikata.smsemailforwarder.data.remote

import android.accounts.AccountManager
import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.repository.OAuthTokenProvider

class AccountManagerOAuthTokenProvider(context: Context) : OAuthTokenProvider {

    private val accountManager: AccountManager = AccountManager.get(context)

    override fun getToken(accountName: String): String {
        val account = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE)
            .firstOrNull { it.name == accountName }
            ?: throw IllegalStateException("Google account not found on device: $accountName")
        val bundle = accountManager.getAuthToken(account, GMAIL_SCOPE, null, false, null, null).result
        return bundle.getString(AccountManager.KEY_AUTHTOKEN)
            ?: throw IllegalStateException(
                "Google auth token unavailable for $accountName. " +
                    "Open SMS Email Forwarder and re-select your Google Account in Settings.",
            )
    }

    override fun invalidateToken(accountName: String, token: String) {
        accountManager.invalidateAuthToken(GOOGLE_ACCOUNT_TYPE, token)
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"
    }
}
