package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.DeleteAccountResult

interface AccountRepository {
    /**
     * Asks the user to sign in again, then deletes their remote data in one atomic write (join key, the households they own
     * for every member, their membership in the others, their profile), then deletes the auth user and finally every local data
     * on the device. The auth user is only deleted after its data.
     *
     * Throws when the confirmation or a deletion fails. A rejected remote write deletes nothing. A remote write that times
     * out (e.g. offline) also throws, but may still be applied later from Firestore's local queue; either way the auth user
     * is kept and retrying is safe. Remaining gap: an item another member adds to an owned household between the read of
     * its items and the commit is left orphaned under the deleted household.
     */
    suspend fun deleteAccount(): DeleteAccountResult
}
