package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.account.AccountDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation

/**
 * Every [deleteAccountData] attempt is recorded **before** [onDelete], [gate], [neverAcknowledge] and [deleteFailure] apply.
 * The batch itself is Firestore code; its permissions are covered by the rules tests in `firestore-tests/`.
 */
class FakeAccountDataSource : AccountDataSource {
    /** When set, [deleteAccountData] throws it (like a batch rejected by the rules): nothing is deleted. */
    var deleteFailure: Throwable? = null

    /** How many attempts succeed before [deleteFailure] applies (e.g. 1 to fail only the second pass). */
    var succeedingAttempts = 0

    /** When set, [deleteAccountData] waits for it to complete, like a slow server acknowledgement. */
    var gate: CompletableDeferred<Unit>? = null

    /** The batch never completes, like a Firestore write while offline. */
    var neverAcknowledge = false

    /** Called on every attempt, so a test can observe what the caller had already done at that moment. */
    var onDelete: () -> Unit = {}

    val deleteAttempts = mutableListOf<String>()

    /** User ids whose data was deleted successfully. */
    val deletedUserIds = mutableListOf<String>()

    override suspend fun deleteAccountData(userId: String) {
        deleteAttempts += userId
        onDelete()
        gate?.await()
        if (neverAcknowledge) awaitCancellation()
        if (deleteAttempts.size > succeedingAttempts) deleteFailure?.let { throw it }
        deletedUserIds += userId
    }
}
