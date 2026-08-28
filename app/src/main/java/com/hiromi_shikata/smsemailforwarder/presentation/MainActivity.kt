package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hiromi_shikata.smsemailforwarder.BuildConfig
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.CacheApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.GithubAppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.databinding.ActivityMainBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ApkDownloadUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun resolveBatteryOptimizationButtonVisibility(isIgnoringBatteryOptimizations: Boolean): Int =
    if (isIgnoringBatteryOptimizations) View.GONE else View.VISIBLE

internal fun resolveGrantPermissionButtonVisibility(granted: Boolean): Int =
    if (granted) View.GONE else View.VISIBLE

internal fun shouldShowUpdateDialog(update: AppUpdate?, hasShownUpdateDialog: Boolean): Boolean =
    update != null && !hasShownUpdateDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var apkDownloadUseCase: ApkDownloadUseCase
    private var hasAutoOpenedSettings = false
    private var hasShownUpdateDialog = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        updatePermissionStatus(permissions.all { it.value })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apkDownloadUseCase = ApkDownloadUseCase(CacheApkDownloadRepository(this))

        val configRepository = SharedPrefsForwardingConfigRepository(this)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                ForwardingConfigGetUseCase(configRepository),
                AppUpdateCheckUseCase(GithubAppUpdateRepository(BuildConfig.GITHUB_REPO)),
                BuildConfig.VERSION_NAME,
            ),
        )[MainViewModel::class.java]

        binding.viewLogButton.setOnClickListener {
            startActivity(Intent(this, ForwardingLogActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.grantPermissionButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        }

        binding.batteryOptimizationButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        }

        binding.updateButton.setOnClickListener {
            viewModel.update.value?.let { update ->
                downloadAndInstall(update.downloadUrl, update.latestVersion)
            }
        }

        viewModel.config.observe(this) { config ->
            binding.statusText.text = when {
                !config.isComplete -> getString(R.string.setup_required)
                config.authMode == EmailAuthMode.GOOGLE_ACCOUNT -> getString(
                    R.string.forwarding_active_google,
                    config.smtpUsername,
                    config.destinationEmail,
                )
                else -> getString(R.string.forwarding_active, config.destinationEmail)
            }
            if (!config.isComplete && !hasAutoOpenedSettings) {
                hasAutoOpenedSettings = true
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        viewModel.update.observe(this) { update ->
            if (update != null) {
                binding.updateButton.visibility = View.VISIBLE
                binding.updateButton.text = getString(R.string.update_available, update.latestVersion)
            } else {
                binding.updateButton.visibility = View.GONE
            }
            if (shouldShowUpdateDialog(update, hasShownUpdateDialog)) {
                hasShownUpdateDialog = true
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_update_dialog_title))
                    .setMessage(getString(R.string.app_update_dialog_message, update!!.latestVersion))
                    .setPositiveButton(getString(R.string.app_update_dialog_positive)) { _, _ ->
                        downloadAndInstall(update.downloadUrl, update.latestVersion)
                    }
                    .setNegativeButton(getString(R.string.app_update_dialog_negative)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        requestRequiredPermissions()
        viewModel.loadConfig()
        viewModel.checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadConfig()
        val smsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        updatePermissionStatus(smsGranted)
        val powerManager = getSystemService<PowerManager>() ?: return
        binding.batteryOptimizationButton.visibility =
            resolveBatteryOptimizationButtonVisibility(
                powerManager.isIgnoringBatteryOptimizations(packageName),
            )
    }

    private fun downloadAndInstall(downloadUrl: String, version: String) {
        binding.updateButton.isEnabled = false
        binding.updateButton.text = getString(R.string.downloading)

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    apkDownloadUseCase.execute(downloadUrl)
                }
                val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.provider",
                    file,
                )
                startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.download_failed),
                    Toast.LENGTH_LONG,
                ).show()
                binding.updateButton.isEnabled = true
                binding.updateButton.text = getString(R.string.update_available, version)
            }
        }
    }

    private fun requestRequiredPermissions() {
        val required = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            updatePermissionStatus(true)
        }
    }

    private fun updatePermissionStatus(granted: Boolean) {
        binding.grantPermissionButton.visibility = resolveGrantPermissionButtonVisibility(granted)
        binding.permissionStatus.visibility = if (granted) View.VISIBLE else View.GONE
        binding.permissionStatus.text = if (granted) {
            getString(R.string.permissions_granted)
        } else {
            getString(R.string.permissions_required)
        }
    }
}
