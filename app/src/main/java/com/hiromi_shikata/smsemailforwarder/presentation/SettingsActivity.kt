package com.hiromi_shikata.smsemailforwarder.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.databinding.ActivitySettingsBinding
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val repository = SharedPrefsForwardingConfigRepository(this)
        viewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(
                ForwardingConfigGetUseCase(repository),
                ForwardingConfigUpdateUseCase(repository),
            ),
        )[SettingsViewModel::class.java]

        binding.saveButton.setOnClickListener {
            viewModel.saveConfig(
                destinationEmail = binding.destinationEmailInput.text.toString(),
                smtpHost = binding.smtpHostInput.text.toString(),
                smtpPort = binding.smtpPortInput.text.toString(),
                smtpUsername = binding.smtpUsernameInput.text.toString(),
                smtpPassword = binding.smtpPasswordInput.text.toString(),
            )
        }

        viewModel.config.observe(this) { config ->
            binding.destinationEmailInput.setText(config.destinationEmail)
            binding.smtpHostInput.setText(config.smtpHost)
            binding.smtpPortInput.setText(config.smtpPort.toString())
            binding.smtpUsernameInput.setText(config.smtpUsername)
            binding.smtpPasswordInput.setText(config.smtpPassword)
        }

        viewModel.saved.observe(this) {
            Toast.makeText(this, getString(com.hiromi_shikata.smsemailforwarder.R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        viewModel.loadConfig()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
