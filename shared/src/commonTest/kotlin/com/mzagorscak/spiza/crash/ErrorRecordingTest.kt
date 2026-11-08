package com.mzagorscak.spiza.crash

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ErrorRecordingTest {
    private val logger = FakeCrashLogger()

    @Test
    fun recordError_logsWarning_whenReasonIsExpected() {
        logger.recordError(context = "Saving", error = DataException(DataException.Reason.Offline, "offline"))

        assertThat(logger.warnings).hasSize(1)
        assertThat(logger.exceptions).isEmpty()
    }

    @Test
    fun recordError_recordsExceptionKeepingOriginalCause_whenWriteFailsForAnUnexpectedReason() {
        // A rejected write and a used-up quota both need attention, so neither may be downgraded to a warning.
        val errors =
            listOf(
                DataException(DataException.Reason.PermissionDenied, "denied"),
                DataException(DataException.Reason.QuotaExceeded, "quota"),
            )

        errors.forEach { logger.recordError(context = "Saving", error = it) }

        assertThat(logger.warnings).isEmpty()
        assertThat(logger.exceptions.map { it.cause?.message }).isEqualTo(errors.map { it.message })
        assertThat(logger.exceptions.map { it.message.orEmpty() }).each { it.startsWith("Saving") }
    }

    @Test
    fun recordError_classifiesWrappedErrors_byTheirCause() {
        val wrapped = RuntimeException("wrapper", DataException(DataException.Reason.Offline, "offline"))

        logger.recordError(context = "Saving", error = wrapped)

        assertThat(logger.warnings).hasSize(1)
        assertThat(logger.exceptions).isEmpty()
    }

    @Test
    fun catchAndRecord_emitsFallbackAndLogsWarning_whenStreamLosesPermission() =
        runTest {
            val stream =
                flow {
                    emit(1)
                    throw DataException(DataException.Reason.PermissionDenied, "removed from household")
                }.catchAndRecord(crashLogger = logger, context = "Items") { emit(-1) }

            stream.test {
                assertThat(awaitItem()).isEqualTo(1)
                assertThat(awaitItem()).isEqualTo(-1)
                awaitComplete()
            }
            assertThat(logger.warnings).hasSize(1)
            assertThat(logger.exceptions).isEmpty()
        }
}
