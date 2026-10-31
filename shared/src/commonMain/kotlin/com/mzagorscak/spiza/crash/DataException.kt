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

        Unknown,
    }
}

/** Classifies this throwable, looking through its causes. */
expect fun Throwable.dataErrorReason(): DataException.Reason
