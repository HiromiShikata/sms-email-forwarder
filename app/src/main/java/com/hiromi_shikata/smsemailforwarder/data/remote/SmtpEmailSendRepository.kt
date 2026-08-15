package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SmtpEmailSendRepository : EmailSendRepository {
    override fun send(message: SmsMessage, config: ForwardingConfig) {
        val props = Properties().apply {
            put("mail.smtp.host", config.smtpHost)
            put("mail.smtp.port", config.smtpPort.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.trust", config.smtpHost)
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(config.smtpUsername, config.smtpPassword)
        })
        val mimeMessage = MimeMessage(session).apply {
            setFrom(InternetAddress(config.smtpUsername))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.destinationEmail))
            subject = "SMS from ${message.sender}"
            setText("From: ${message.sender}\n\n${message.body}")
        }
        Transport.send(mimeMessage)
    }
}
