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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.hiromi_shikata.smsemailforwarder.APP_UPDATE_IMMEDIATE_WORK_NAME
import com.hiromi_shikata.smsemailforwarder.BuildConfig
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsAppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.local.SmtpPasswordUnavailableException
import com.hiromi_shikata.smsemailforwarder.databinding.ActivityMainBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateStatusGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.enqueueImmediateAppUpdateCheck
import java.text.DateFormat
import java.util.Date

internal fun resolveBatteryOptimizationButtonVisibility(isIgnoringBatteryOptimizations: Boolean): Int =
    if (isIgnoringBatteryOptimizations) View.GONE else View.VISIBLE

internal fun resolveGrantPermissionButtonVisibility(granted: Boolean): Int =
    if (granted) View.GONE else View.VISIBLE

internal fun resolvePermissionGrantStatus(permissions: Map<String, Boolean>): Boolean =
    permissions.all { it.value }

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var hasAutoOpenedSettings = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        updatePermissionStatus(missingPermissions())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        val configRepository = SharedPrefsForwardingConfigRepository(this)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                ForwardingConfigGetUseCase(configRepository),
                AppUpdateStatusGetUseCase(SharedPrefsAppUpdateStatusRepository(this)),
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
            requestRequiredPermissions()
        }

        binding.batteryOptimizationButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        }

        binding.updateButton.setOnClickListener {
            startActivity(Intent(this, AppUpdateInstallActivity::class.java))
        }

        viewModel.config.observe(this) { config ->
            binding.statusText.text = when {
                !config.isComplete -> getString(R.string.setup_required)
                config.authMode == EmailAuthMode.GOOGLE_ACCOUNT -> getString(
                    R.string.forwarding_active_google,
                    config.smtpUsername,
                    config.destinationEmail,
                )
                config.authMode == EmailAuthMode.HTTPS_API -> getString(R.string.forwarding_active_https_api)
                else -> getString(R.string.forwarding_active, config.destinationEmail)
            }
            if (!config.isComplete && !hasAutoOpenedSettings) {
                hasAutoOpenedSettings = true
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        viewModel.updateStatus.observe(this) { view -> renderUpdateStatus(view) }

        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(APP_UPDATE_IMMEDIATE_WORK_NAME)
            .observe(this) { viewModel.refreshUpdateStatus() }

        requestRequiredPermissions()
        loadConfigOrShowError()
    }

    override fun onResume() {
        super.onResume()
        loadConfigOrShowError()
        viewModel.refreshUpdateStatus()
        enqueueImmediateAppUpdateCheck(this)
        updatePermissionStatus(missingPermissions())
        val powerManager = getSystemService<PowerManager>() ?: return
        binding.batteryOptimizationButton.visibility =
            resolveBatteryOptimizationButtonVisibility(
                powerManager.isIgnoringBatteryOptimizations(packageName),
            )
    }

    private fun applySystemBarInsets() {
        val basePadding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(
                left = basePadding + bars.left,
                top = basePadding + bars.top,
                right = basePadding + bars.right,
                bottom = basePadding + bars.bottom,
            )
            insets
        }
    }

    private fun renderUpdateStatus(view: AppUpdateStatusView) {
        binding.updateStatusText.text = when (view) {
            is AppUpdateStatusView.NotChecked -> getString(
                R.string.app_update_status_not_checked,
                view.installedVersion,
            )
            is AppUpdateStatusView.UpToDate -> getString(
                R.string.app_update_status_up_to_date,
                view.installedVersion,
                formatLocalTime(view.checkedAtEpochMillis),
            )
            is AppUpdateStatusView.Available -> getString(
                R.string.app_update_status_available,
                view.installedVersion,
                view.latestVersion,
            )
            is AppUpdateStatusView.Downloading -> getString(
                R.string.app_update_status_downloading,
                view.installedVersion,
                view.latestVersion,
            )
            is AppUpdateStatusView.ReadyToInstall -> view.lastCheckFailure?.let { failure ->
                getString(
                    R.string.app_update_status_ready_with_failure,
                    view.installedVersion,
                    view.latestVersion,
                    formatLocalTime(failure.failedAtEpochMillis),
                    failure.message,
                )
            } ?: getString(
                R.string.app_update_status_ready,
                view.installedVersion,
                view.latestVersion,
            )
            is AppUpdateStatusView.CheckFailed -> getString(
                R.string.app_update_status_check_failed,
                view.installedVersion,
                formatLocalTime(view.failedAtEpochMillis),
                view.message,
            )
            is AppUpdateStatusView.ReadFailed -> getString(
                R.string.app_update_status_read_failed,
                view.installedVersion,
                view.message,
            )
        }
        if (view is AppUpdateStatusView.ReadyToInstall) {
            binding.updateButton.visibility = View.VISIBLE
            binding.updateButton.text = getString(R.string.app_update_install_button, view.latestVersion)
        } else {
            binding.updateButton.visibility = View.GONE
        }
    }

    private fun formatLocalTime(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

    private fun loadConfigOrShowError() {
        try {
            viewModel.loadConfig()
        } catch (e: SmtpPasswordUnavailableException) {
            Toast.makeText(this, getString(R.string.smtp_password_unavailable), Toast.LENGTH_LONG).show()
        }
    }

    private fun missingPermissions(): List<String> =
        missingRequiredPermissions(Build.VERSION.SDK_INT) { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestRequiredPermissions() {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            updatePermissionStatus(emptyList())
        }
    }

    private fun updatePermissionStatus(missing: List<String>) {
        binding.grantPermissionButton.visibility = resolveGrantPermissionButtonVisibility(missing.isEmpty())
        binding.permissionStatus.visibility = View.VISIBLE
        binding.permissionStatus.text = when (val status = resolvePermissionStatusText(missing)) {
            PermissionStatusText.Granted -> getString(R.string.permissions_granted)
            is PermissionStatusText.Missing -> when {
                status.sms && status.notifications -> getString(R.string.permissions_missing_sms_and_notifications)
                status.sms -> getString(R.string.permissions_missing_sms)
                else -> getString(R.string.permissions_missing_notifications)
            }
        }
    }
}
