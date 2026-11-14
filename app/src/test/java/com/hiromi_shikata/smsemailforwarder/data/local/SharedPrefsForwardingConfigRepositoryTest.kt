package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.SharedPreferences
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SharedPrefsForwardingConfigRepositoryTest {

    @Test
    fun `get returns the saved config including the password for every case`() {
        val cases = listOf(
            RoundTripCase("SMTP with ASCII password", configWithPassword(EmailAuthMode.SMTP, "smtp-secret-1")),
            RoundTripCase("GOOGLE_ACCOUNT with ASCII password", configWithPassword(EmailAuthMode.GOOGLE_ACCOUNT, "app-password-2")),
            RoundTripCase("SMTP with non-ASCII password", configWithPassword(EmailAuthMode.SMTP, "パスワード 秘密!")),
            RoundTripCase("SMTP with empty password", configWithPassword(EmailAuthMode.SMTP, "")),
            RoundTripCase("EMPTY default config", ForwardingConfig.EMPTY),
        )
        for (case in cases) {
            val repository = SharedPrefsForwardingConfigRepository(FakeSharedPreferences(), FakeSmtpPasswordCipher())
            repository.save(case.config)
            assertEquals(case.label, case.config, repository.get())
        }
    }

    @Test
    fun `saved password is not stored in plain text and is stored as cipher output`() {
        val cipher = FakeSmtpPasswordCipher()
        val passwords = listOf("plain-secret-1", "パスワード-2", "p@ss w0rd!#")
        for (password in passwords) {
            val prefs = FakeSharedPreferences()
            SharedPrefsForwardingConfigRepository(prefs, cipher)
                .save(configWithPassword(EmailAuthMode.SMTP, password))

            assertFalse(
                "plain password must not appear in any stored value: $password",
                prefs.getAll().values.any { it == password },
            )
            assertEquals(
                "encrypted password must be the cipher output: $password",
                cipher.encrypt(password),
                prefs.getString("smtp_password_encrypted", null),
            )
        }
    }

    @Test
    fun `get decrypts the password stored under the encrypted key`() {
        val cipher = FakeSmtpPasswordCipher()
        val passwords = listOf("stored-secret-1", "保存済みパスワード")
        for (password in passwords) {
            val prefs = FakeSharedPreferences()
            prefs.edit().putString("smtp_password_encrypted", cipher.encrypt(password)).apply()

            val repository = SharedPrefsForwardingConfigRepository(prefs, cipher)

            assertEquals(password, repository.get().smtpPassword)
        }
    }

    @Test
    fun `get migrates a legacy plain password to encrypted storage and keeps returning it`() {
        val cipher = FakeSmtpPasswordCipher()
        val legacyPasswords = listOf("legacy-secret-1", "旧パスワード")
        for (password in legacyPasswords) {
            val prefs = FakeSharedPreferences()
            prefs.edit().putString("smtp_password", password).apply()
            val repository = SharedPrefsForwardingConfigRepository(prefs, cipher)

            assertEquals("first get: $password", password, repository.get().smtpPassword)
            assertFalse("legacy key must be removed: $password", prefs.contains("smtp_password"))
            assertEquals(
                "encrypted key must hold cipher output: $password",
                cipher.encrypt(password),
                prefs.getString("smtp_password_encrypted", null),
            )
            assertEquals("second get: $password", password, repository.get().smtpPassword)
        }
    }

    @Test
    fun `save removes the legacy plain password key and stores only the new password encrypted`() {
        val cipher = FakeSmtpPasswordCipher()
        val cases = listOf(
            LegacySaveCase(legacyPassword = "old-secret-1", newPassword = "new-secret-1"),
            LegacySaveCase(legacyPassword = "旧パスワード", newPassword = "新パスワード"),
        )
        for (case in cases) {
            val prefs = FakeSharedPreferences()
            prefs.edit().putString("smtp_password", case.legacyPassword).apply()
            val repository = SharedPrefsForwardingConfigRepository(prefs, cipher)

            repository.save(configWithPassword(EmailAuthMode.SMTP, case.newPassword))

            assertFalse("legacy key must be removed: ${case.legacyPassword}", prefs.contains("smtp_password"))
            assertFalse(
                "legacy plain password must not remain in any stored value: ${case.legacyPassword}",
                prefs.getAll().values.any { it == case.legacyPassword },
            )
            assertEquals(case.newPassword, repository.get().smtpPassword)
        }
    }

    @Test
    fun `get returns an empty password when no password was ever stored`() {
        val repository = SharedPrefsForwardingConfigRepository(FakeSharedPreferences(), FakeSmtpPasswordCipher())

        assertEquals("", repository.get().smtpPassword)
    }

    @Test
    fun `empty password round-trips through save and get`() {
        val repository = SharedPrefsForwardingConfigRepository(FakeSharedPreferences(), FakeSmtpPasswordCipher())
        val config = configWithPassword(EmailAuthMode.SMTP, "")

        repository.save(config)

        assertEquals("", repository.get().smtpPassword)
        assertEquals(config, repository.get())
    }

    private fun configWithPassword(authMode: EmailAuthMode, password: String) = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = authMode,
        smtpHost = "smtp.example.net",
        smtpPort = 465,
        smtpUsername = "sender@example.net",
        smtpPassword = password,
    )
}

private data class RoundTripCase(val label: String, val config: ForwardingConfig)

private data class LegacySaveCase(val legacyPassword: String, val newPassword: String)

private class FakeSmtpPasswordCipher : SmtpPasswordCipher {
    override fun encrypt(plainText: String): String = PREFIX + plainText.reversed()

    override fun decrypt(encryptedText: String): String {
        require(encryptedText.startsWith(PREFIX)) { "value was not produced by FakeSmtpPasswordCipher" }
        return encryptedText.removePrefix(PREFIX).reversed()
    }

    private companion object {
        const val PREFIX = "enc:"
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = LinkedHashMap<String, Any?>()

    override fun getAll(): Map<String, *> = LinkedHashMap(values)

    override fun getString(key: String, defValue: String?): String? =
        if (values.containsKey(key)) values[key] as String? else defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        unsupported("getStringSet")

    override fun getInt(key: String, defValue: Int): Int =
        if (values.containsKey(key)) values[key] as Int else defValue

    override fun getLong(key: String, defValue: Long): Long = unsupported("getLong")

    override fun getFloat(key: String, defValue: Float): Float = unsupported("getFloat")

    override fun getBoolean(key: String, defValue: Boolean): Boolean = unsupported("getBoolean")

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        unsupported("registerOnSharedPreferenceChangeListener")
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        unsupported("unregisterOnSharedPreferenceChangeListener")
    }

    private fun unsupported(name: String): Nothing =
        throw UnsupportedOperationException("$name is not used by the repository under test")

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableListOf<(MutableMap<String, Any?>) -> Unit>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pending.add { it[key] = value }
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor =
            unsupported("putStringSet")

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pending.add { it[key] = value }
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = unsupported("putLong")

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = unsupported("putFloat")

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            unsupported("putBoolean")

        override fun remove(key: String): SharedPreferences.Editor {
            pending.add { it.remove(key) }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            pending.add { it.clear() }
            return this
        }

        override fun commit(): Boolean {
            applyPending()
            return true
        }

        override fun apply() {
            applyPending()
        }

        private fun applyPending() {
            pending.forEach { it(values) }
            pending.clear()
        }
    }
}
