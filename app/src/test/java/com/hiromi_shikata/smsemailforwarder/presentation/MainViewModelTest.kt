package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateStatusGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeStatus(var result: () -> AppUpdateStatus?) : AppUpdateStatusRepository {
        override fun load(): AppUpdateStatus? = result()
        override fun save(status: AppUpdateStatus) {
            result = { status }
        }
    }

    private fun viewModel(status: FakeStatus): MainViewModel = MainViewModel(
        configGetUseCase = ForwardingConfigGetUseCase(unusedConfigRepository),
        updateStatusGetUseCase = AppUpdateStatusGetUseCase(status),
        currentVersion = "0.1.344",
    )

    @Test
    fun `refresh shows the stored status, not checked, and a read failure visibly`() {
        val cases = listOf(
            "nothing stored" to FakeStatus { null },
            "stored failure" to FakeStatus { AppUpdateStatus.CheckFailed("0.1.344", "HTTP 403", 3L) },
            "unreadable store" to FakeStatus { throw IllegalArgumentException("Unknown update status type: X") },
        )
        val expected = listOf(
            AppUpdateStatusView.NotChecked("0.1.344"),
            AppUpdateStatusView.CheckFailed("0.1.344", "HTTP 403", 3L),
            AppUpdateStatusView.ReadFailed("0.1.344", "Unknown update status type: X"),
        )
        cases.zip(expected).forEach { (case, want) ->
            val vm = viewModel(case.second)
            vm.refreshUpdateStatus()
            assertEquals(case.first, want, vm.updateStatus.value)
        }
    }

    private val unusedConfigRepository = object : ForwardingConfigRepository {
        override fun get(): ForwardingConfig = throw UnsupportedOperationException("not used")
        override fun save(config: ForwardingConfig) = throw UnsupportedOperationException("not used")
    }
}
