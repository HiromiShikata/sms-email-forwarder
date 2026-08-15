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
            ?: throw IllegalStateException("Failed to obtain OAuth token for $accountName")
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"
    }
}
