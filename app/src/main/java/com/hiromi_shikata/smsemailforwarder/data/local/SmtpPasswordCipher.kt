package com.hiromi_shikata.smsemailforwarder.data.local

interface SmtpPasswordCipher {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedText: String): String
}
