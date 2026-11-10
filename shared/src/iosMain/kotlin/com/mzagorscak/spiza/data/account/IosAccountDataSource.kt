package com.mzagorscak.spiza.data.account

import com.mzagorscak.spiza.data.toDataException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosAccountDataSource(
    private val bridge: IosAccountBridge,
) : AccountDataSource {
    override suspend fun deleteAccountData(userId: String) {
        suspendCancellableCoroutine { continuation ->
            bridge.deleteAccountData(userId = userId) { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(error.toDataException())
                }
            }
        }
    }
}
