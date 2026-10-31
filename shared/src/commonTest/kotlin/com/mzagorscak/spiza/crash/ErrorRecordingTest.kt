package com.mzagorscak.spiza.crash

import com.mzagorscak.spiza.fakes.FakeCrashLogger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
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
    fun recordError_recordsExceptionKeepingOriginalCause_whenReasonIsNotExpected() {
        val error = DataException(DataException.Reason.PermissionDenied, "denied")

        logger.recordError(context = "Saving", error = error)

        assertTrue(logger.warnings.isEmpty())
        val recorded = logger.exceptions.single()
        assertSame(error, recorded.cause)
        assertTrue(recorded.message.orEmpty().startsWith("Saving"))
    }

    @Test
    fun recordError_classifiesWrappedErrors_byTheirCause() {
        val wrapped = RuntimeException("wrapper", DataException(DataException.Reason.Offline, "offline"))

        logger.recordError(context = "Saving", error = wrapped)

        assertEquals(1, logger.warnings.size)
        assertTrue(logger.exceptions.isEmpty())
    }

    @Test
    fun recordError_recordsException_whenErrorIsUnknown() {
        logger.recordError(context = "Saving", error = IllegalStateException("bug"))

        assertEquals(1, logger.exceptions.size)
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
    fun catchAndRecord_recordsException_whenStreamFailsUnexpectedly() =
        runTest {
            val values =
                flow<Int> { throw IllegalStateException("bug") }
                    .catchAndRecord(crashLogger = logger, context = "Items")
                    .toList()

            assertTrue(values.isEmpty())
            assertEquals(1, logger.exceptions.size)
        }
}
