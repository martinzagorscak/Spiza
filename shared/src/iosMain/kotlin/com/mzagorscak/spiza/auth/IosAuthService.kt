package com.mzagorscak.spiza.auth

import com.mzagorscak.spiza.crash.CrashLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosAuthService(
    private val bridge: IosAuthBridge,
    private val crashLogger: CrashLogger,
) : AuthService {
    override val authState: Flow<AuthUser?> =
        callbackFlow {
            bridge.startObservingAuthState { trySend(it) }
            awaitClose { bridge.stopObservingAuthState() }
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
}
