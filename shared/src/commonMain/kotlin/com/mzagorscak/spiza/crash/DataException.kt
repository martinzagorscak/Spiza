package com.mzagorscak.spiza.crash

/**
 * A data source failure with a platform-independent [reason], so callers can decide how to log and present it
 * without knowing about Firebase. Platform SDK exceptions (e.g. `FirebaseFirestoreException` on Android) are
 * classified by [dataErrorReason] without being wrapped; iOS bridges report their errors as [DataException].
 */
class DataException(
    val reason: Reason,
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Reason {
        /** No connection, or the backend did not respond in time. */
        Offline,

        /** Rejected by Firestore security rules (or the user is not signed in). */
        PermissionDenied,

        /** The requested document does not exist. */
        NotFound,

        /**
         * Firestore `RESOURCE_EXHAUSTED` (gRPC 8): the project's read/write quota or spending limit is used up.
         * Retrying immediately does not help; it resets later (daily quota) or after the plan is upgraded.
         */
        QuotaExceeded,

        Unknown,
    }
}

/** Classifies this throwable, looking through its causes. */
expect fun Throwable.dataErrorReason(): DataException.Reason

/** True when this failure (or one of its causes) means the Firestore quota is used up, see [DataException.Reason.QuotaExceeded]. */
fun Throwable.isQuotaExceeded(): Boolean = dataErrorReason() == DataException.Reason.QuotaExceeded
