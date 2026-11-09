package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.repository.AccountRepository
import kotlinx.coroutines.awaitCancellation

/** Every [deleteAccount] attempt is recorded **before** [deleteFailure] / [neverCompleteDelete] apply. */
class FakeAccountRepository : AccountRepository {
    /** What [deleteAccount] returns. */
    var deleteAccountResult: DeleteAccountResult = DeleteAccountResult.Deleted

    /** When set, [deleteAccount] throws it. */
    var deleteFailure: Throwable? = null

    /** [deleteAccount] never completes, like a confirmation the user has not answered yet. */
    var neverCompleteDelete = false

    var deleteAccountCalls = 0

    override suspend fun deleteAccount(): DeleteAccountResult {
        deleteAccountCalls++
        deleteFailure?.let { throw it }
        if (neverCompleteDelete) awaitCancellation()
        return deleteAccountResult
    }
}
