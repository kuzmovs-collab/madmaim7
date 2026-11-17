package com.example.meterphoto

import android.content.Context
import android.content.SharedPreferences

/** Хранение настроек почты в SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mail_settings", Context.MODE_PRIVATE)

    var recipient: String
        get() = prefs.getString("recipient", "") ?: ""
        set(v) = prefs.edit().putString("recipient", v).apply()

    var senderEmail: String
        get() = prefs.getString("sender_email", "") ?: ""
        set(v) = prefs.edit().putString("sender_email", v).apply()

    var senderPassword: String
        get() = prefs.getString("sender_password", "") ?: ""
        set(v) = prefs.edit().putString("sender_password", v).apply()

    var smtpHost: String
        get() = prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com"
        set(v) = prefs.edit().putString("smtp_host", v).apply()

    var smtpPort: String
        get() = prefs.getString("smtp_port", "587") ?: "587"
        set(v) = prefs.edit().putString("smtp_port", v).apply()

    /** Автоматически делать снимок, когда штрихкод распознан */
    var autoCapture: Boolean
        get() = prefs.getBoolean("auto_capture", true)
        set(v) = prefs.edit().putBoolean("auto_capture", v).apply()

    val isConfigured: Boolean
        get() = recipient.isNotEmpty() && senderEmail.isNotEmpty() && senderPassword.isNotEmpty()
}
