package com.mzagorscak.spiza.data.account

import com.mzagorscak.spiza.data.BridgeError

/** Implemented in Swift (iosApp), which owns FirebaseFirestore. Must behave like `FirestoreAccountDataSource`. */
interface IosAccountBridge {
    /** See [AccountDataSource.deleteAccountData]: `.server` reads, then one batch. [error] is null when everything was deleted. */
    fun deleteAccountData(
        userId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )
}
