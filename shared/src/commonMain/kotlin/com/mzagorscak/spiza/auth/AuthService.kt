package com.mzagorscak.spiza.auth

import kotlinx.coroutines.flow.Flow

/**
 * Apple Auth Provider should be implemented within Firebase console
 * Since I need to enroll as Apple Developer (and pay membership), I will implement it later when it needs to be published on Apple App Store
 * TODO: #ImplementAppleAuthProvider
 */
enum class AuthProvider { Google, Apple }

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)

sealed interface AuthResult {
    data class Success(
        val user: AuthUser,
    ) : AuthResult

    data class Cancelled(
        val message: String? = null,
    ) : AuthResult

    data class Failure(
        val error: Throwable,
    ) : AuthResult
}

/** [AuthService.reauthenticate] got an account other than the signed-in one (Firebase user mismatch); reported as [AuthResult.Failure]. */
class UserMismatchException(
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause)

interface AuthService {
    /** Emits the signed-in user, or null when signed out. */
    val authState: Flow<AuthUser?>

    val currentUser: AuthUser?

    suspend fun signIn(provider: AuthProvider): AuthResult

    suspend fun signOut()

    /**
     * Re-runs the sign-in flow of the provider the current user signed in with and re-authenticates them, because
     * Firebase only allows [deleteCurrentUser] after a recent login. Picking a different account than the signed-in
     * one is a [AuthResult.Failure] with a [UserMismatchException]; no signed-in user is a [AuthResult.Failure] too.
     */
    suspend fun reauthenticate(): AuthResult

    /**
     * Deletes the Firebase Auth user (throws when that fails, e.g. offline or the login is not recent enough) and then
     * revokes the provider session; a failure to revoke it is only logged as a warning.
     */
    suspend fun deleteCurrentUser()
}
