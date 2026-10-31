package com.mzagorscak.spiza.crash

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch

/** Which [DataException.Reason]s are normal app situations (logged as warnings) rather than bugs. */
object ExpectedErrors {
    /** A rejected write means a client bug or a rules mismatch, so only being offline is expected. */
    val writes: Set<DataException.Reason> = setOf(DataException.Reason.Offline)

    /** Listeners lose access when membership changes (e.g. the user was removed from a household). */
    val streams: Set<DataException.Reason> = setOf(DataException.Reason.Offline, DataException.Reason.PermissionDenied)
}

/**
 * Logs [error] that happened in [context] (e.g. "Settings sign-out failed"): reasons in [expected] become a warning,
 * anything else is recorded as a non-fatal exception that keeps [error] as its cause. Never put user content in [context].
 */
fun CrashLogger.recordError(
    context: String,
    error: Throwable,
    expected: Set<DataException.Reason> = ExpectedErrors.writes,
) {
    val reason = error.dataErrorReason()
    if (reason in expected) {
        logWarning(message = "$context ($reason): ${error.message}")
    } else {
        recordException(Throwable(message = "$context; ${error.message}", cause = error))
    }
}

/**
 * Catches an upstream failure of this flow, logs it with [recordError] and lets [onError] emit a fallback
 * (e.g. an error state or an empty value). Cancellation is not caught.
 */
fun <T> Flow<T>.catchAndRecord(
    crashLogger: CrashLogger,
    context: String,
    expected: Set<DataException.Reason> = ExpectedErrors.streams,
    onError: suspend FlowCollector<T>.(Throwable) -> Unit = {},
): Flow<T> =
    catch { error ->
        crashLogger.recordError(context = context, error = error, expected = expected)
        onError(error)
    }
