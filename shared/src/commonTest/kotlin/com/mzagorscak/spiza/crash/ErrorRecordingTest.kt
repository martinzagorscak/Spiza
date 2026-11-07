package com.mzagorscak.spiza.crash

import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorRecordingTest {
    private val logger = FakeCrashLogger()

    @Test
    fun recordError_logsWarning_whenReasonIsExpected() {
        logger.recordError(context = "Saving", error = DataException(DataException.Reason.Offline, "offline"))

        assertEquals(1, logger.warnings.size)
        assertTrue(logger.exceptions.isEmpty())
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

        assertTrue(logger.warnings.isEmpty())
        assertEquals(errors, logger.exceptions.map { it.cause })
        assertTrue(logger.exceptions.all { it.message.orEmpty().startsWith("Saving") })
    }

    @Test
    fun recordError_classifiesWrappedErrors_byTheirCause() {
        val wrapped = RuntimeException("wrapper", DataException(DataException.Reason.Offline, "offline"))

        logger.recordError(context = "Saving", error = wrapped)

        assertEquals(1, logger.warnings.size)
        assertTrue(logger.exceptions.isEmpty())
    }

    @Test
    fun catchAndRecord_emitsFallbackAndLogsWarning_whenStreamLosesPermission() =
        runTest {
            val values =
                flow {
                    emit(1)
                    throw DataException(DataException.Reason.PermissionDenied, "removed from household")
                }.catchAndRecord(crashLogger = logger, context = "Items") { emit(-1) }
                    .toList()

            assertEquals(listOf(1, -1), values)
            assertEquals(1, logger.warnings.size)
            assertTrue(logger.exceptions.isEmpty())
        }

    @Test
    fun catchAndRecord_recordsException_whenStreamFailsWithQuotaExceeded() =
        runTest {
            val values =
                flow<Int> { throw DataException(DataException.Reason.QuotaExceeded, "quota") }
                    .catchAndRecord(crashLogger = logger, context = "Items") { emit(-1) }
                    .toList()

            assertEquals(listOf(-1), values)
            assertTrue(logger.warnings.isEmpty())
            assertEquals(1, logger.exceptions.size)
        }

    @Test
    fun isQuotaExceeded_isTrue_whenQuotaExceededIsTheErrorOrANestedCause() {
        val quota = DataException(DataException.Reason.QuotaExceeded, "quota")

        assertTrue(quota.isQuotaExceeded())
        assertTrue(RuntimeException("outer", IllegalStateException("middle", quota)).isQuotaExceeded())
    }

    @Test
    fun isQuotaExceeded_isFalse_whenReasonIsNotQuotaExceeded() {
        DataException.Reason.entries.filter { it != DataException.Reason.QuotaExceeded }.forEach { reason ->
            assertFalse(DataException(reason, "failure").isQuotaExceeded(), "$reason must not be a quota failure")
        }
    }
}
