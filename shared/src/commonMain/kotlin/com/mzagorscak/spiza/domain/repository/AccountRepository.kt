package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.DeleteAccountResult

interface AccountRepository {
    /**
     * Asks the user to sign in again, then deletes their join key, the households they own (for every member), leaves the
     * others, deletes their profile, the auth user and finally every local data on the device.
     * Throws when the confirmation or a deletion fails (e.g. offline); the auth user is only deleted after its data.
     */
    suspend fun deleteAccount(): DeleteAccountResult
}
