package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class SmtpEmailSendRepository : EmailSendRepository {
    override fun send(message: SmsMessage, config: ForwardingConfig) {
        val session = Session.getInstance(buildSmtpSessionProperties(config), object : Authenticator() {
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
