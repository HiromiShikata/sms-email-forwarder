package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.BuildConfig
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.DownloadManagerApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.GithubAppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.databinding.ActivityMainBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ApkDownloadUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var apkDownloadUseCase: ApkDownloadUseCase

    private var pendingDownloadId: Long = -1L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        updatePermissionStatus(permissions.all { it.value })
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == pendingDownloadId) {
                installDownloadedApk(id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apkDownloadUseCase = ApkDownloadUseCase(DownloadManagerApkDownloadRepository(this))

        val configRepository = SharedPrefsForwardingConfigRepository(this)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                ForwardingConfigGetUseCase(configRepository),
                AppUpdateCheckUseCase(GithubAppUpdateRepository(BuildConfig.GITHUB_REPO)),
                BuildConfig.VERSION_NAME,
            ),
        )[MainViewModel::class.java]

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.updateButton.setOnClickListener {
            viewModel.update.value?.let { update ->
                pendingDownloadId = apkDownloadUseCase.execute(
                    update.downloadUrl,
                    "sms-email-forwarder_universal.apk",
                )
            }
        }

        viewModel.config.observe(this) { config ->
            binding.statusText.text = when {
                !config.isComplete -> getString(R.string.setup_required)
                config.authMode == EmailAuthMode.GOOGLE_ACCOUNT -> getString(
                    R.string.forwarding_active_google,
                    config.googleAccountName,
                    config.destinationEmail,
                )
                else -> getString(R.string.forwarding_active, config.destinationEmail)
            }
        }

        viewModel.update.observe(this) { update ->
            if (update != null) {
                binding.updateButton.visibility = View.VISIBLE
                binding.updateButton.text = getString(R.string.update_available, update.latestVersion)
            } else {
                binding.updateButton.visibility = View.GONE
            }
        }

        requestRequiredPermissions()
        viewModel.loadConfig()
        viewModel.checkForUpdate()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED,
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            )
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(downloadCompleteReceiver)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadConfig()
    }

    private fun installDownloadedApk(downloadId: Long) {
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId) ?: return
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(installIntent)
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
        binding.permissionStatus.text = if (granted) {
            getString(R.string.permissions_granted)
        } else {
            getString(R.string.permissions_required)
        }
    }
}
