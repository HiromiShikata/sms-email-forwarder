package com.hiromi_shikata.smsemailforwarder.presentation

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.databinding.ActivitySettingsBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: return@registerForActivityResult
            binding.selectedAccountText.text = accountName
        }
    }

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
            updateSectionVisibility(checkedId == R.id.authModeGoogleAccount)
        }

        binding.chooseAccountButton.setOnClickListener {
            val intent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"),
                null, null, null, null,
            )
            accountPickerLauncher.launch(intent)
        }

        binding.saveButton.setOnClickListener {
            if (binding.authModeGoogleAccount.isChecked) {
                val accountName = binding.selectedAccountText.text.toString()
                if (accountName == getString(R.string.no_account_selected)) {
                    Toast.makeText(this, getString(R.string.choose_google_account), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.saveGoogleAccountConfig(
                    destinationEmail = binding.destinationEmailInput.text.toString(),
                    googleAccountName = accountName,
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
            when (config.authMode) {
                EmailAuthMode.SMTP -> {
                    binding.authModeSmtp.isChecked = true
                    updateSectionVisibility(false)
                }
                EmailAuthMode.GOOGLE_ACCOUNT -> {
                    binding.authModeGoogleAccount.isChecked = true
                    updateSectionVisibility(true)
                    if (config.googleAccountName.isNotBlank()) {
                        binding.selectedAccountText.text = config.googleAccountName
                    }
                }
            }
        }

        viewModel.saved.observe(this) {
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        viewModel.loadConfig()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun updateSectionVisibility(googleAccountMode: Boolean) {
        binding.smtpSection.visibility = if (googleAccountMode) View.GONE else View.VISIBLE
        binding.googleAccountSection.visibility = if (googleAccountMode) View.VISIBLE else View.GONE
    }
}
