package com.mzagorscak.spiza.domain.model

sealed interface DeleteAccountResult {
    /** The account, its remote data and every local data on the device are deleted; the user is signed out. */
    data object Deleted : DeleteAccountResult

    /** The user dismissed the sign-in confirmation; nothing was deleted. */
    data object Cancelled : DeleteAccountResult

    /** The user confirmed with an account other than the signed-in one; nothing was deleted. */
    data object WrongAccount : DeleteAccountResult

    data object NotSignedIn : DeleteAccountResult
}
