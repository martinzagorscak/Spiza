package com.mzagorscak.spiza.ui.screens.common

import androidx.lifecycle.ViewModel
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
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
    fun launchSafely_recordsErrorAndCallsOnError_whenBlockThrows() =
        runTest {
            val error = IllegalStateException("write rejected")
            var reported: Throwable? = null

            viewModel.launchSafely(crashLogger = logger, context = "Saving", onError = { reported = it }) { throw error }

            assertSame(error, reported)
            assertSame(error, logger.exceptions.single().cause)
        }

    @Test
    fun launchSafely_doesNotReportCancellation() =
        runTest {
            var errorCount = 0

            val job = viewModel.launchSafely(crashLogger = logger, context = "Saving", onError = { errorCount++ }) { awaitCancellation() }
            job.cancel()

            assertEquals(0, errorCount)
            assertTrue(logger.exceptions.isEmpty() && logger.warnings.isEmpty())
        }
}
