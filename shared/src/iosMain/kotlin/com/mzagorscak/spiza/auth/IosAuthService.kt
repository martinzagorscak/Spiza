package com.mzagorscak.spiza.auth

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.toDataException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosAuthService(
    private val bridge: IosAuthBridge,
    private val crashLogger: CrashLogger,
) : AuthService {
    override val authState: Flow<AuthUser?> =
        callbackFlow {
            val handle = bridge.observeAuthState { trySend(it) }
            awaitClose { handle.remove() }
        }

    override val currentUser: AuthUser? get() = bridge.currentUser()

    override suspend fun signIn(provider: AuthProvider): AuthResult =
        suspendCancellableCoroutine { continuation ->
            bridge.signIn(provider) { user, cancelled, errorMessage ->
                continuation.resume(
                    when {
                        user != null -> AuthResult.Success(user)
                        cancelled -> AuthResult.Cancelled(errorMessage)
                        else -> AuthResult.Failure(IllegalStateException(errorMessage ?: "Sign-in failed"))
                    },
                )
            }
        }

    override suspend fun signOut() {
        suspendCancellableCoroutine { continuation ->
            bridge.signOut { errorMessage ->
                continuation.resume(errorMessage)
            }
        }.let { error ->
            if (error != null) {
                crashLogger.recordException(Throwable(message = "Sign-out failed; $error"))
            }
        }
    }

    override suspend fun reauthenticate(): AuthResult =
        suspendCancellableCoroutine { continuation ->
            bridge.reauthenticate { user, cancelled, error ->
                continuation.resume(
                    when {
                        user != null -> AuthResult.Success(user)
                        cancelled -> AuthResult.Cancelled(error?.message)
                        error?.reason == AuthBridgeErrors.USER_MISMATCH ->
                            AuthResult.Failure(UserMismatchException(message = error.message))

                        else ->
                            AuthResult.Failure(
                                error?.toDataException()
                                    ?: DataException(reason = DataException.Reason.Unknown, message = "Reauthentication failed"),
                            )
                    },
                )
            }
        }

    override suspend fun deleteCurrentUser() {
        val sessionRevokeError =
            suspendCancellableCoroutine { continuation ->
                bridge.deleteCurrentUser { error, sessionRevokeError ->
                    if (error == null) {
                        continuation.resume(sessionRevokeError)
                    } else {
                        continuation.resumeWithException(error.toDataException())
                    }
                }
            }
        // The Firebase user is already gone; a Google session left behind only affects the next account picker.
        if (sessionRevokeError != null) {
            crashLogger.logWarning(message = "Disconnecting Google session failed: ${sessionRevokeError.message}")
        }
    }
}
