package com.mzagorscak.spiza.ui.screens.appUpdate

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.single
import com.mzagorscak.spiza.domain.model.AppUpdateStatus
import com.mzagorscak.spiza.domain.model.AppVersionConfig
import com.mzagorscak.spiza.domain.usecase.DismissRecommendedUpdateUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetAppUpdateStatusUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeAppUpdateRepository
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val INSTALLED = 10_203L
private const val RECOMMENDED = INSTALLED + 1

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelImplTest {
    private val repository = FakeAppUpdateRepository().apply { installedVersion = INSTALLED }
    private val crashLogger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        AppUpdateViewModelImpl(
            getAppUpdateStatus = GetAppUpdateStatusUseCaseImpl(repository = repository),
            dismissRecommendedUpdate = DismissRecommendedUpdateUseCaseImpl(repository = repository),
            crashLogger = crashLogger,
        )

    private fun recommendUpdate() {
        repository.versionConfig = AppVersionConfig(minSupportedVersion = 0, recommendedVersion = RECOMMENDED)
    }

    private fun requireUpdate() {
        repository.versionConfig = AppVersionConfig(minSupportedVersion = INSTALLED + 1, recommendedVersion = 0)
    }

    @Test
    fun onAppStarted_showsTheCheckedStatus() =
        runTest {
            requireUpdate()
            val viewModel = viewModel()

            viewModel.onAppStarted()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun onAppStarted_ignoresACallWhileACheckIsRunning() =
        runTest {
            requireUpdate()
            val slowFetch = CompletableDeferred<Unit>()
            repository.versionConfigGate = slowFetch
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onAppStarted()
            slowFetch.complete(Unit)

            assertThat(repository.versionConfigRequests).isEqualTo(1)
            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun onAppStarted_keepsARecommendationHiddenThisSession_butAlwaysShowsARequiredUpdate() =
        runTest {
            recommendUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()
            viewModel.onHideRecommended()

            viewModel.onAppStarted()
            val afterRecheck = viewModel.state().value.status
            requireUpdate()
            viewModel.onAppStarted()

            assertThat(afterRecheck).isEqualTo(AppUpdateStatus.UpToDate)
            assertThat(repository.dismissedVersion).isNull()
            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun onUpdateClick_hidesTheRecommendation_withoutStoringItAsDismissed() =
        runTest {
            recommendUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onUpdateClick()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpToDate)
            assertThat(repository.dismissedVersion).isNull()
        }

    @Test
    fun onUpdateClick_keepsTheRequiredUpdateShown() =
        runTest {
            requireUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onUpdateClick()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun onHideRecommended_keepsTheRequiredUpdateShown() =
        runTest {
            requireUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onHideRecommended()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
        }

    @Test
    fun onDismissRecommended_hidesTheRecommendation_andStoresItsVersion() =
        runTest {
            recommendUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onDismissRecommended()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpToDate)
            assertThat(repository.dismissedAttempts).containsExactly(RECOMMENDED)
        }

    // region Error handling

    @Test
    fun onAppStarted_failsOpen_andRecordsTheFailure_whenTheCheckFails() =
        runTest {
            val failure = IllegalStateException("boom")
            requireUpdate()
            repository.versionConfigFailure = failure
            val viewModel = viewModel()

            viewModel.onAppStarted()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpToDate)
            assertThat(crashLogger.exceptions).single().transform { it.cause?.message }.isEqualTo(failure.message)
        }

    @Test
    fun onAppStarted_keepsTheRequiredUpdate_andRecordsTheFailure_whenARecheckFails() =
        runTest {
            val failure = IllegalStateException("boom")
            requireUpdate()
            val viewModel = viewModel()
            viewModel.onAppStarted()
            repository.versionConfigFailure = failure

            viewModel.onAppStarted()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpdateRequired)
            assertThat(crashLogger.exceptions).single().transform { it.cause?.message }.isEqualTo(failure.message)
        }

    @Test
    fun onDismissRecommended_keepsTheDialogClosed_andRecordsTheFailure_whenSavingFails() =
        runTest {
            val failure = IllegalStateException("disk full")
            recommendUpdate()
            repository.writeFailure = failure
            val viewModel = viewModel()
            viewModel.onAppStarted()

            viewModel.onDismissRecommended()

            assertThat(viewModel.state().value.status).isEqualTo(AppUpdateStatus.UpToDate)
            assertThat(crashLogger.exceptions).single().transform { it.cause?.message }.isEqualTo(failure.message)
            assertThat(crashLogger.warnings).isEmpty()
        }

    // endregion
}
