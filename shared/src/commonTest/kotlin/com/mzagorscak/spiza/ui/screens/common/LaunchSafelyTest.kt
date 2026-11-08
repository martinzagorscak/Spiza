package com.mzagorscak.spiza.ui.screens.common

import androidx.lifecycle.ViewModel
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchSafelyTest {
    private class TestViewModel : ViewModel()

    private val logger = FakeCrashLogger()
    private val viewModel = TestViewModel()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun launchSafely_runsTheBlock_withoutReportingAnError_whenItSucceeds() =
        runTest {
            var blockRan = false
            var errorCount = 0

            viewModel.launchSafely(crashLogger = logger, context = "Saving", onError = { errorCount++ }) { blockRan = true }

            assertThat(blockRan).isTrue()
            assertThat(errorCount).isEqualTo(0)
            assertThat(logger.exceptions).isEmpty()
            assertThat(logger.warnings).isEmpty()
        }

    // region Error handling

    @Test
    fun launchSafely_recordsErrorAndCallsOnError_whenBlockThrows() =
        runTest {
            val error = IllegalStateException("write rejected")
            var reported: Throwable? = null

            viewModel.launchSafely(crashLogger = logger, context = "Saving", onError = { reported = it }) { throw error }

            assertThat(reported?.message).isEqualTo(error.message)
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(error.message)
        }

    @Test
    fun launchSafely_doesNotReportCancellation() =
        runTest {
            var errorCount = 0

            val job = viewModel.launchSafely(crashLogger = logger, context = "Saving", onError = { errorCount++ }) { awaitCancellation() }
            job.cancel()

            assertThat(errorCount).isEqualTo(0)
            assertThat(logger.exceptions).isEmpty()
            assertThat(logger.warnings).isEmpty()
        }

    // endregion
}
