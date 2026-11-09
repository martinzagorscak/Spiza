package com.mzagorscak.spiza.crash

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

actual fun Throwable.dataErrorReason(): DataException.Reason =
    generateSequence(this) { it.cause }
        .firstNotNullOfOrNull { it.knownReason() }
        ?: DataException.Reason.Unknown

private fun Throwable.knownReason(): DataException.Reason? =
    when (this) {
        is DataException -> reason
        is IOException -> DataException.Reason.Offline
        // Firebase Auth (e.g. reauthenticate, delete user) reports a missing connection or timeout with this exception.
        is FirebaseNetworkException -> DataException.Reason.Offline
        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> DataException.Reason.Offline

                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
                -> DataException.Reason.PermissionDenied

                FirebaseFirestoreException.Code.NOT_FOUND -> DataException.Reason.NotFound
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> DataException.Reason.QuotaExceeded
                else -> null
            }

        else -> null
    }
