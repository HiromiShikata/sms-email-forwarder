package com.hiromi_shikata.smsemailforwarder.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SmtpPasswordUnavailableException
import com.hiromi_shikata.smsemailforwarder.databinding.ActivitySettingsBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
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

        binding.authModeGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            showAuthSection(
                when (checkedId) {
                    R.id.authModeSmtp -> EmailAuthMode.SMTP
                    R.id.authModeHttpsApi -> EmailAuthMode.HTTPS_API
                    else -> EmailAuthMode.GOOGLE_ACCOUNT
                },
            )
        }

        binding.getAppPasswordButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords")))
        }

        binding.testGoogleAccountButton.setOnClickListener {
            binding.testGoogleAccountButton.isEnabled = false
            binding.testGoogleAccountButton.text = getString(R.string.test_connection_testing)
            viewModel.testGoogleAccountConfig(
                destinationEmail = binding.destinationEmailInput.text.toString(),
                gmailAddress = binding.gmailAddressInput.text.toString(),
                appPassword = binding.gmailAppPasswordInput.text.toString(),
            )
        }

        binding.testSmtpButton.setOnClickListener {
            binding.testSmtpButton.isEnabled = false
            binding.testSmtpButton.text = getString(R.string.test_connection_testing)
            viewModel.testSmtpConfig(
                destinationEmail = binding.destinationEmailInput.text.toString(),
                smtpHost = binding.smtpHostInput.text.toString(),
                smtpPort = binding.smtpPortInput.text.toString(),
                smtpUsername = binding.smtpUsernameInput.text.toString(),
                smtpPassword = binding.smtpPasswordInput.text.toString(),
            )
        }

        binding.saveButton.setOnClickListener {
            if (binding.authModeHttpsApi.isChecked) {
                val saved = viewModel.saveHttpsApiConfig(
                    apiEndpointUrl = binding.apiEndpointUrlInput.text.toString(),
                    apiKey = binding.apiKeyInput.text.toString(),
                )
                if (!saved) {
                    Toast.makeText(this, getString(R.string.api_endpoint_must_be_https), Toast.LENGTH_LONG).show()
                }
            } else if (binding.authModeGoogleAccount.isChecked) {
                viewModel.saveGoogleAccountConfig(
                    destinationEmail = binding.destinationEmailInput.text.toString(),
                    gmailAddress = binding.gmailAddressInput.text.toString(),
                    appPassword = binding.gmailAppPasswordInput.text.toString(),
                )
            } else {
                viewModel.saveSmtpConfig(
                    destinationEmail = binding.destinationEmailInput.text.toString(),
                    smtpHost = binding.smtpHostInput.text.toString(),
                    smtpPort = binding.smtpPortInput.text.toString(),
                    smtpUsername = binding.smtpUsernameInput.text.toString(),
                    smtpPassword = binding.smtpPasswordInput.text.toString(),
                )
            }
        }

        viewModel.config.observe(this) { config ->
            binding.destinationEmailInput.setText(config.destinationEmail)
            binding.smtpHostInput.setText(config.smtpHost)
            binding.smtpPortInput.setText(config.smtpPort.toString())
            binding.smtpUsernameInput.setText(config.smtpUsername)
            binding.smtpPasswordInput.setText(config.smtpPassword)
            binding.apiEndpointUrlInput.setText(config.apiEndpointUrl)
            binding.apiKeyInput.setText(config.apiKey)
            when (config.authMode) {
                EmailAuthMode.SMTP -> {
                    binding.authModeSmtp.isChecked = true
                    showAuthSection(EmailAuthMode.SMTP)
                }
                EmailAuthMode.GOOGLE_ACCOUNT -> {
                    binding.authModeGoogleAccount.isChecked = true
                    showAuthSection(EmailAuthMode.GOOGLE_ACCOUNT)
                    binding.gmailAddressInput.setText(config.smtpUsername)
                    binding.gmailAppPasswordInput.setText(config.smtpPassword)
                }
                EmailAuthMode.HTTPS_API -> {
                    binding.authModeHttpsApi.isChecked = true
                    showAuthSection(EmailAuthMode.HTTPS_API)
                }
            }
        }

        viewModel.saved.observe(this) {
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        viewModel.testResult.observe(this) { result ->
            val (title, message) = if (result.isSuccess) {
                getString(R.string.test_connection_success_title) to
                    getString(R.string.test_connection_success_message)
            } else {
                getString(R.string.test_connection_failure_title) to
                    (result.exceptionOrNull()?.message ?: getString(R.string.test_connection_unknown_error))
            }
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            binding.testGoogleAccountButton.isEnabled = true
            binding.testGoogleAccountButton.text = getString(R.string.test_connection)
            binding.testSmtpButton.isEnabled = true
            binding.testSmtpButton.text = getString(R.string.test_connection)
        }

        loadConfigOrShowError()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadConfigOrShowError() {
        try {
            viewModel.loadConfig()
        } catch (e: SmtpPasswordUnavailableException) {
            Toast.makeText(this, getString(R.string.smtp_password_unavailable), Toast.LENGTH_LONG).show()
        }
    }

    private fun showAuthSection(authMode: EmailAuthMode) {
        binding.smtpSection.visibility = visibilityOf(authMode == EmailAuthMode.SMTP)
        binding.googleAccountSection.visibility = visibilityOf(authMode == EmailAuthMode.GOOGLE_ACCOUNT)
        binding.httpsApiSection.visibility = visibilityOf(authMode == EmailAuthMode.HTTPS_API)
    }

    private fun visibilityOf(shown: Boolean): Int = if (shown) View.VISIBLE else View.GONE
}
