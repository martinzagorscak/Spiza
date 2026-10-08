package com.mzagorscak.spiza.auth

import kotlinx.coroutines.flow.Flow

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

interface AuthService {
    /** Emits the signed-in user, or null when signed out. */
    val authState: Flow<AuthUser?>

    val currentUser: AuthUser?

    suspend fun signIn(provider: AuthProvider): AuthResult

    suspend fun signOut()
}
