package com.example.meterphoto

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.meterphoto.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var store: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = SettingsStore(this)

        binding.etRecipient.setText(store.recipient)
        binding.etSenderEmail.setText(store.senderEmail)
        binding.etSenderPassword.setText(store.senderPassword)
        binding.etSmtpHost.setText(store.smtpHost)
        binding.etSmtpPort.setText(store.smtpPort)
        binding.swAutoCapture.isChecked = store.autoCapture

        binding.btnSave.setOnClickListener {
            store.recipient = binding.etRecipient.text.toString().trim()
            store.senderEmail = binding.etSenderEmail.text.toString().trim()
            store.senderPassword = binding.etSenderPassword.text.toString().trim()
            store.smtpHost = binding.etSmtpHost.text.toString().trim()
            store.smtpPort = binding.etSmtpPort.text.toString().trim()
            store.autoCapture = binding.swAutoCapture.isChecked
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
