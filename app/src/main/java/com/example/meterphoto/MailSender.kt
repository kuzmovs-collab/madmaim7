package com.example.meterphoto

import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Отправка письма с вложением через SMTP.
 * ВНИМАНИЕ: для Gmail/Yandex нужен не пароль аккаунта,
 * а пароль приложения (задаётся в настройках безопасности почты).
 * Вызов выполнять НЕ на главном потоке (вызывается из SendWorker).
 */
object MailSender {

    fun send(
        to: String,
        from: String,
        password: String,
        subject: String,
        body: String,
        file: File,
        smtpHost: String = "smtp.gmail.com",
        smtpPort: String = "587"
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(from, password)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)

            val multipart = MimeMultipart().apply {
                addBodyPart(MimeBodyPart().apply { setText(body) })
                addBodyPart(MimeBodyPart().apply { attachFile(file) })
            }
            setContent(multipart)
        }

        Transport.send(message)
    }
}
